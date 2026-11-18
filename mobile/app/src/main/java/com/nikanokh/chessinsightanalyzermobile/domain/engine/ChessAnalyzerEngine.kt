package com.nikanokh.chessinsightanalyzermobile.domain.engine

import com.nikanokh.chessinsightanalyzermobile.domain.analysis.AccuracyCalculator
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessBoardState
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessEngine
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessMove
import com.nikanokh.chessinsightanalyzermobile.domain.chess.EcoOpeningBook
import com.nikanokh.chessinsightanalyzermobile.domain.model.ClassificationThresholds
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineConfig
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineEvaluation
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineLine
import com.nikanokh.chessinsightanalyzermobile.domain.model.GameStatistics
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveAnalysis
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveTreeNode
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 1:1 port of Web `src/core/analysis/MoveAnalyzer.ts`
 * Includes:
 * - Static Exchange Evaluation (`evaluateSeeOnSquare`, `doesMoveLosePiece`, `isMaterialSacrificeMove`)
 * - Exact 11-category classification (`classifyMoveFromEvaluations`)
 * - Instant threshold reclassification (`reclassifyMoveAnalyses`)
 * - Full game analysis pipeline using `StockfishUciBridge` (with deterministic fallback for JVM unit tests)
 */
object ChessAnalyzerEngine {

    val DEFAULT_THRESHOLDS = ClassificationThresholds(
        bestMaxCpl = 20,
        excellentMaxCpl = 50,
        goodMaxCpl = 95,
        inaccuracyMaxCpl = 175,
        mistakeMaxCpl = 300
    )

    private val PIECE_VALUES: Map<PieceType, Int> = mapOf(
        PieceType.PAWN to 100,
        PieceType.KNIGHT to 300,
        PieceType.BISHOP to 310,
        PieceType.ROOK to 500,
        PieceType.QUEEN to 900,
        PieceType.KING to 10000
    )

    private fun pieceValue(type: PieceType): Int = PIECE_VALUES[type] ?: 0

    /**
     * Converts an `EngineEvaluation` into centipawns from the moving player's perspective.
     * Matches Web `evalToMoverCp`.
     */
    fun evalToMoverCp(ev: EngineEvaluation, moverColor: PieceColor): Int {
        val multiplier = if (moverColor == PieceColor.WHITE) 1 else -1
        if (ev.mate != null) {
            val m = ev.mate * multiplier
            return if (m > 0) 9900 - m * 20 else -9900 - m * 20
        }
        return (ev.cp ?: 0) * multiplier
    }

    /**
     * Recursive Static Exchange Evaluation (SEE) on `targetSquare` for `sideToMove`.
     * Always explores the least-valuable attacker (`LVA`) first.
     * Matches Web `evaluateSeeOnSquare` in `src/core/analysis/MoveAnalyzer.ts`.
     */
    fun evaluateSeeOnSquare(board: ChessBoardState, targetSquare: Int): Int {
        val capturesToTarget = ChessEngine.generateLegalMoves(board)
            .filter { it.to == targetSquare }
            .sortedBy { m ->
                val piece = board.pieceAt(m.from)
                piece?.let { pieceValue(it.type) } ?: 0
            }

        if (capturesToTarget.isEmpty()) return 0

        val lvaCapture = capturesToTarget.first()
        val capturedPieceType = when {
            lvaCapture.isEnPassant -> PieceType.PAWN
            else -> lvaCapture.capturedPiece?.type ?: board.pieceAt(targetSquare)?.type ?: return 0
        }

        val capturedVal = pieceValue(capturedPieceType)
        val promoGain = if (lvaCapture.promotion != null) {
            pieceValue(lvaCapture.promotion) - pieceValue(PieceType.PAWN)
        } else {
            0
        }

        val nextBoard = ChessEngine.applyMove(board, lvaCapture)
        val opponentGain = evaluateSeeOnSquare(nextBoard, targetSquare)

        return max(0, capturedVal + promoGain - opponentGain)
    }

    /**
     * Determines whether a move from `fenBefore` (`fromSquare` -> `toSquare`)
     * loses material after Static Exchange Evaluation on `toSquare` or leaves
     * another friendly piece hanging (`>= 200 cp`).
     * Matches Web `doesMoveLosePiece` in `src/core/analysis/MoveAnalyzer.ts`.
     */
    fun doesMoveLosePiece(
        fenBefore: String,
        fromSquare: String,
        toSquare: String,
        promotion: Char? = null
    ): Boolean {
        return try {
            val boardBefore = ChessBoardState.fromFen(fenBefore)
            val fromIdx = ChessEngine.algebraicToSquare(fromSquare)
            val toIdx = ChessEngine.algebraicToSquare(toSquare)
            if (fromIdx !in 0..63 || toIdx !in 0..63) return false

            val legalMoves = ChessEngine.generateLegalMoves(boardBefore)
            val move = legalMoves.firstOrNull { m ->
                m.from == fromIdx && m.to == toIdx &&
                    (promotion == null || m.promotion?.code == promotion.lowercaseChar())
            } ?: legalMoves.firstOrNull { it.from == fromIdx && it.to == toIdx } ?: return false

            val movedPiece = boardBefore.pieceAt(fromIdx) ?: return false
            val immediateGain = when {
                move.isEnPassant -> pieceValue(PieceType.PAWN)
                move.capturedPiece != null -> pieceValue(move.capturedPiece.type)
                else -> 0
            }
            val promoGain = if (move.promotion != null) {
                pieceValue(move.promotion) - pieceValue(PieceType.PAWN)
            } else {
                0
            }

            val boardAfter = ChessEngine.applyMove(boardBefore, move)

            // 1. Check SEE on the destination square (`toSquare`)
            val opponentSeeGainOnDest = evaluateSeeOnSquare(boardAfter, toIdx)
            val netExchangeOnDest = immediateGain + promoGain - opponentSeeGainOnDest
            if (netExchangeOnDest <= -150) {
                return true
            }

            // 2. Check if the move exposed another friendly minor/major piece to a profitable capture
            if (movedPiece.type != PieceType.KING && immediateGain == 0) {
                val oppMoves = ChessEngine.generateLegalMoves(boardAfter)
                val targetSquares = LinkedHashSet<Int>()
                for (oppMove in oppMoves) {
                    if (oppMove.capturedPiece != null && oppMove.to != toIdx) {
                        targetSquares.add(oppMove.to)
                    }
                }
                for (sq in targetSquares) {
                    val seeGain = evaluateSeeOnSquare(boardAfter, sq)
                    if (seeGain >= 200) {
                        return true
                    }
                }
            }

            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if a move is a material sacrifice (either moving onto a square where SEE is negative
     * or leaving a friendly piece en prise with SEE >= 200 cp).
     * Matches Web `isMaterialSacrificeMove` in `src/core/analysis/MoveAnalyzer.ts`.
     */
    fun isMaterialSacrificeMove(
        fenBefore: String,
        fromSquare: String,
        toSquare: String,
        promotion: Char? = null
    ): Boolean {
        return doesMoveLosePiece(fenBefore, fromSquare, toSquare, promotion)
    }

    data class ClassificationResult(
        val category: MoveCategory,
        val cpLoss: Int,
        val winProbBefore: Double,
        val winProbAfter: Double,
        val winProbLoss: Double,
        val isOnlyBestMove: Boolean,
        val isMissedTactic: Boolean,
        val explanation: String
    )

    /**
     * Core pure move classification function matching Web `classifyMoveFromEvaluations`
     * in `src/core/analysis/MoveAnalyzer.ts` 1:1.
     */
    fun classifyMoveFromEvaluations(
        evalBefore: EngineEvaluation,
        evalAfter: EngineEvaluation,
        moverColor: PieceColor,
        playedUci: String,
        playedSan: String,
        bestMoveUci: String,
        bestMoveSan: String,
        isBook: Boolean,
        legalMovesCount: Int,
        topLinesBefore: List<EngineLine>,
        isSacrifice: Boolean,
        thresholds: ClassificationThresholds = DEFAULT_THRESHOLDS,
        didBestMoveLosePiece: Boolean = false,
        didPlayedMoveLosePiece: Boolean = false
    ): ClassificationResult {
        val cpBefore = evalToMoverCp(evalBefore, moverColor)
        val cpAfter = evalToMoverCp(evalAfter, moverColor)
        val cpLoss = max(0, cpBefore - cpAfter)

        val winProb = AccuracyCalculator.computeMoveWinProbLoss(evalBefore, evalAfter, moverColor)
        val winProbBefore = winProb.winProbBefore
        val winProbAfter = winProb.winProbAfter
        val winProbLoss = winProb.winProbLoss

        // Convert thresholds into approximate winProbLoss thresholds scaled by user settings
        val scale = thresholds.bestMaxCpl.toDouble() / DEFAULT_THRESHOLDS.bestMaxCpl.toDouble()
        val bestMaxWpl = 1.8 * scale
        val excellentMaxWpl = 4.8 * (thresholds.excellentMaxCpl.toDouble() / DEFAULT_THRESHOLDS.excellentMaxCpl.toDouble())
        val goodMaxWpl = 9.2 * (thresholds.goodMaxCpl.toDouble() / DEFAULT_THRESHOLDS.goodMaxCpl.toDouble())
        val inaccuracyMaxWpl = 17.5 * (thresholds.inaccuracyMaxCpl.toDouble() / DEFAULT_THRESHOLDS.inaccuracyMaxCpl.toDouble())
        val mistakeMaxWpl = 28.5 * (thresholds.mistakeMaxCpl.toDouble() / DEFAULT_THRESHOLDS.mistakeMaxCpl.toDouble())

        // Check if only one move maintains advantage (MultiPV comparison)
        var isOnlyBestMove = false
        if (topLinesBefore.size >= 2) {
            val line1Eval = EngineEvaluation(cp = topLinesBefore[0].cp, mate = topLinesBefore[0].mate, depth = topLinesBefore[0].depth, pv = topLinesBefore[0].pvUci)
            val line2Eval = EngineEvaluation(cp = topLinesBefore[1].cp, mate = topLinesBefore[1].mate, depth = topLinesBefore[1].depth, pv = topLinesBefore[1].pvUci)
            val w1 = AccuracyCalculator.getPlayerWinProbability(
                AccuracyCalculator.cpToWhiteWinProbability(line1Eval.cp, line1Eval.mate),
                moverColor
            )
            val w2 = AccuracyCalculator.getPlayerWinProbability(
                AccuracyCalculator.cpToWhiteWinProbability(line2Eval.cp, line2Eval.mate),
                moverColor
            )
            if (w1 - w2 >= 14.0 && w1 >= 42.0) {
                isOnlyBestMove = true
            }
        }

        val isBestMovePlayed = playedUci == bestMoveUci || (cpLoss <= 12 && winProbLoss <= bestMaxWpl)

        // Check mate-specific scenarios from mover's perspective
        val moverMultiplier = if (moverColor == PieceColor.WHITE) 1 else -1
        val mateBefore = if (evalBefore.mate != null) evalBefore.mate * moverMultiplier else null
        val mateAfter = if (evalAfter.mate != null) evalAfter.mate * moverMultiplier else null

        val hadForcedMate = mateBefore != null && mateBefore > 0
        val stillHasForcedMate = mateAfter != null && mateAfter > 0
        val steppedIntoForcedMate = (mateBefore == null || mateBefore >= 0) && (mateAfter != null && mateAfter < 0)

        // Missed tactic: player had a clear winning tactic or forced mate and dropped it
        val missedForcedMateToEqualOrWorse = hadForcedMate && !stillHasForcedMate && winProbAfter < 82.0
        val missedCrushingTactic =
            !hadForcedMate &&
                isOnlyBestMove &&
                !isBestMovePlayed &&
                winProbBefore >= 68.0 &&
                winProbAfter >= 35.0 &&
                winProbLoss >= 16.0 &&
                !didBestMoveLosePiece

        val isMissedTactic = missedForcedMateToEqualOrWorse || missedCrushingTactic

        // 1. Theoretical Book Move
        if (isBook) {
            return ClassificationResult(
                category = MoveCategory.BOOK,
                cpLoss = 0,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = 0.0,
                isOnlyBestMove = false,
                isMissedTactic = false,
                explanation = "Теоретический дебютный ход ($playedSan) из справочника дебютов."
            )
        }

        // 2. Forced Move (only 1 legal move in position)
        if (legalMovesCount <= 1) {
            return ClassificationResult(
                category = MoveCategory.FORCED,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = true,
                isMissedTactic = false,
                explanation = "Единственный легальный ход в данной позиции ($playedSan)."
            )
        }

        // 3. Stepped into forced checkmate when not previously being mated
        if (steppedIntoForcedMate && winProbBefore >= 15.0) {
            return ClassificationResult(
                category = MoveCategory.BLUNDER,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = false,
                explanation = "Грубая ошибка (зевок): ход $playedSan позволяет сопернику объявить форсированный мат (${abs(mateAfter!!)} х.). Лучше было $bestMoveSan."
            )
        }

        // 4. Missed forced mate
        if (hadForcedMate && !stillHasForcedMate) {
            if (winProbAfter < 30.0) {
                return ClassificationResult(
                    category = MoveCategory.BLUNDER,
                    cpLoss = cpLoss,
                    winProbBefore = winProbBefore,
                    winProbAfter = winProbAfter,
                    winProbLoss = winProbLoss,
                    isOnlyBestMove = isOnlyBestMove,
                    isMissedTactic = true,
                    explanation = "Грубая ошибка: упущен форсированный мат в $mateBefore х. (лучший ход $bestMoveSan) и позиция стала проигранной."
                )
            }
            return ClassificationResult(
                category = MoveCategory.MISSED_CHANCE,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = true,
                explanation = "Упущенная победа: в позиции был форсированный мат в $mateBefore х. после хода $bestMoveSan."
            )
        }

        // 4b. Kept forced mate, check if it found the fastest mate or delayed it
        if (hadForcedMate && stillHasForcedMate) {
            val extraMateMoves = max(0, mateAfter!! - (mateBefore!! - 1))
            if (playedUci == bestMoveUci || extraMateMoves == 0) {
                if (isSacrifice && mateBefore <= 6) {
                    return ClassificationResult(
                        category = MoveCategory.BRILLIANT,
                        cpLoss = 0,
                        winProbBefore = winProbBefore,
                        winProbAfter = winProbAfter,
                        winProbLoss = 0.0,
                        isOnlyBestMove = isOnlyBestMove,
                        isMissedTactic = false,
                        explanation = "Бриллиантовая матовая комбинация с жертвой ($playedSan)! Форсированный мат в $mateAfter х."
                    )
                }
                return ClassificationResult(
                    category = MoveCategory.BEST,
                    cpLoss = 0,
                    winProbBefore = winProbBefore,
                    winProbAfter = winProbAfter,
                    winProbLoss = 0.0,
                    isOnlyBestMove = isOnlyBestMove,
                    isMissedTactic = false,
                    explanation = "Точный матовый ход ($playedSan), ведущий к кратчайшему форсированному мату в $mateAfter х."
                )
            }
            if (extraMateMoves <= 2) {
                return ClassificationResult(
                    category = MoveCategory.EXCELLENT,
                    cpLoss = min(cpLoss, thresholds.excellentMaxCpl),
                    winProbBefore = winProbBefore,
                    winProbAfter = winProbAfter,
                    winProbLoss = 0.0,
                    isOnlyBestMove = isOnlyBestMove,
                    isMissedTactic = false,
                    explanation = "Отличный ход ($playedSan), сохраняющий форсированный мат в $mateAfter х. (кратчайший путь — $bestMoveSan)."
                )
            }
            return ClassificationResult(
                category = MoveCategory.GOOD,
                cpLoss = min(cpLoss, thresholds.goodMaxCpl),
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = 0.0,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = false,
                explanation = "Хороший ход ($playedSan), сохраняющий форсированный мат, хотя $bestMoveSan вело к мату быстрее."
            )
        }

        // 5. Brilliant Move (!!)
        if (
            isSacrifice &&
            isBestMovePlayed &&
            winProbLoss <= bestMaxWpl + 1.5 &&
            winProbAfter >= 45.0 &&
            winProbBefore <= 92.0
        ) {
            return ClassificationResult(
                category = MoveCategory.BRILLIANT,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = false,
                explanation = "Бриллиантовый ход ($playedSan)! Корректная жертва материала с сильной инициативой."
            )
        }

        // 6. Great Move (!)
        if (isOnlyBestMove && isBestMovePlayed && winProbLoss <= bestMaxWpl + 1.5) {
            return ClassificationResult(
                category = MoveCategory.GREAT,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = true,
                isMissedTactic = false,
                explanation = "Сильный ключевой ход ($playedSan)! Единственное продолжение, удерживающее перевес."
            )
        }

        // 7. Best Move (★)
        if (playedUci == bestMoveUci || (winProbLoss <= bestMaxWpl && cpLoss <= thresholds.bestMaxCpl)) {
            return ClassificationResult(
                category = MoveCategory.BEST,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = false,
                explanation = "Лучший ход по первой линии движка Stockfish ($playedSan)."
            )
        }

        // 8. Missed Chance (✖)
        if (isMissedTactic && winProbLoss <= mistakeMaxWpl) {
            return ClassificationResult(
                category = MoveCategory.MISSED_CHANCE,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = true,
                explanation = "Упущенная тактическая возможность: ход $bestMoveSan давал решающий перевес."
            )
        }

        // 9. Excellent Move (👍)
        if (winProbLoss <= excellentMaxWpl && cpLoss <= thresholds.excellentMaxCpl) {
            return ClassificationResult(
                category = MoveCategory.EXCELLENT,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = false,
                explanation = "Отличный ход ($playedSan), практически не уступающий первой линии движка ($bestMoveSan)."
            )
        }

        // 10. Good Move (✓)
        if (winProbLoss <= goodMaxWpl && cpLoss <= thresholds.goodMaxCpl) {
            return ClassificationResult(
                category = MoveCategory.GOOD,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = false,
                explanation = "Хороший добротный ход ($playedSan). Немного точнее было $bestMoveSan."
            )
        }

        // 11. Inaccuracy (?!)
        if (winProbLoss <= inaccuracyMaxWpl && cpLoss <= thresholds.inaccuracyMaxCpl && !didPlayedMoveLosePiece) {
            return ClassificationResult(
                category = MoveCategory.INACCURACY,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = isMissedTactic,
                explanation = "Неточность ($playedSan, потеря ~${(cpLoss / 100.0).format1()} пешки). Сильнее было $bestMoveSan."
            )
        }

        // 12. Mistake (?)
        if (winProbLoss <= mistakeMaxWpl && cpLoss <= thresholds.mistakeMaxCpl) {
            return ClassificationResult(
                category = MoveCategory.MISTAKE,
                cpLoss = cpLoss,
                winProbBefore = winProbBefore,
                winProbAfter = winProbAfter,
                winProbLoss = winProbLoss,
                isOnlyBestMove = isOnlyBestMove,
                isMissedTactic = isMissedTactic,
                explanation = if (didPlayedMoveLosePiece) {
                    "Ошибка ($playedSan): ход ведёт к потере материала. Следовало играть $bestMoveSan."
                } else {
                    "Ошибка ($playedSan, потеря ~${(cpLoss / 100.0).format1()} пешки). Следовало играть $bestMoveSan."
                }
            )
        }

        // 13. Blunder (??)
        return ClassificationResult(
            category = MoveCategory.BLUNDER,
            cpLoss = cpLoss,
            winProbBefore = winProbBefore,
            winProbAfter = winProbAfter,
            winProbLoss = winProbLoss,
            isOnlyBestMove = isOnlyBestMove,
            isMissedTactic = isMissedTactic,
            explanation = if (didPlayedMoveLosePiece) {
                "Зевок фигуры или материала ($playedSan, потеря ~${(cpLoss / 100.0).format1()} пешки)! Лучшим ходом было $bestMoveSan."
            } else {
                "Грубая ошибка / зевок ($playedSan, потеря ~${(cpLoss / 100.0).format1()} пешки)! Лучшим ходом было $bestMoveSan."
            }
        )
    }

    private fun Double.format1(): String = String.format(java.util.Locale.US, "%.1f", this)

    /**
     * Re-evaluates the categories, explanations, and accuracies of an already-analyzed move list
     * when the user changes `ClassificationThresholds` in Settings without re-running Stockfish.
     * Matches Web `reclassifyMoveAnalyses` in `src/core/analysis/MoveAnalyzer.ts`.
     */
    fun reclassifyMoveAnalyses(
        analyses: List<MoveAnalysis>,
        thresholds: ClassificationThresholds
    ): List<MoveAnalysis> {
        return analyses.map { m ->
            val evalBefore = m.evalBefore
            val evalAfter = m.evalAfter
            if (evalBefore == null || evalAfter == null) return@map m

            val bestUci = m.bestMoveUci ?: m.uci
            val bestFrom = if (bestUci.length >= 2) bestUci.substring(0, 2) else m.from
            val bestTo = if (bestUci.length >= 4) bestUci.substring(2, 4) else m.to
            val bestPromo = if (bestUci.length >= 5) bestUci[4] else null

            val didBestMoveLosePiece = doesMoveLosePiece(m.fenBefore, bestFrom, bestTo, bestPromo)
            val didPlayedMoveLosePiece = doesMoveLosePiece(m.fenBefore, m.from, m.to, m.promotion)

            val classified = classifyMoveFromEvaluations(
                evalBefore = evalBefore,
                evalAfter = evalAfter,
                moverColor = m.color,
                playedUci = m.uci,
                playedSan = m.san,
                bestMoveUci = bestUci,
                bestMoveSan = m.bestMoveSan ?: m.san,
                isBook = m.isBook,
                legalMovesCount = m.legalMovesCount,
                topLinesBefore = m.topLines,
                isSacrifice = m.isSacrifice,
                thresholds = thresholds,
                didBestMoveLosePiece = didBestMoveLosePiece,
                didPlayedMoveLosePiece = didPlayedMoveLosePiece
            )

            val accuracy = AccuracyCalculator.calculateSingleMoveAccuracy(
                winProbLoss = classified.winProbLoss,
                isBook = m.isBook,
                isForced = classified.category == MoveCategory.FORCED
            )

            m.copy(
                cpLoss = classified.cpLoss,
                winProbBefore = classified.winProbBefore,
                winProbAfter = classified.winProbAfter,
                winProbLoss = classified.winProbLoss,
                accuracy = accuracy,
                category = classified.category,
                isOnlyBestMove = classified.isOnlyBestMove,
                isMissedTactic = classified.isMissedTactic,
                explanation = classified.explanation
            )
        }
    }

    /**
     * Full game analysis pipeline using `StockfishUciBridge` (or fallback search if WebView is unavailable,
     * e.g., inside local JVM unit tests).
     * Matches Web `analyzeFullGame` in `src/core/analysis/MoveAnalyzer.ts`.
     */
    suspend fun analyzeFullGame(
        nodes: List<MoveTreeNode>,
        config: EngineConfig = EngineConfig(),
        thresholds: ClassificationThresholds = DEFAULT_THRESHOLDS,
        stockfishBridge: StockfishUciBridge? = null,
        onProgress: ((currentPly: Int, totalPlies: Int, moveAnalysis: MoveAnalysis) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false }
    ): Pair<List<MoveAnalysis>, GameStatistics> {
        if (nodes.isEmpty()) {
            return emptyList<MoveAnalysis>() to AccuracyCalculator.calculateGameStatistics(emptyList())
        }

        val totalPlies = nodes.size
        val results = ArrayList<MoveAnalysis>(totalPlies)

        stockfishBridge?.applyConfig(config)

        suspend fun evaluateFen(fen: String): StockfishUciBridge.StockfishAnalysisResult {
            val fromBridge = stockfishBridge?.evaluatePosition(fen, config)
            if (fromBridge != null) return fromBridge
            return evaluatePositionFallback(fen, config)
        }

        var prevResult = evaluateFen(nodes[0].fenBefore)
        var inBookChain = true

        for (i in 0 until totalPlies) {
            if (shouldCancel()) break

            val node = nodes[i]
            val evalBefore = prevResult.evaluation
            val topLinesBefore = prevResult.lines
            val bestMoveUci = prevResult.bestMoveUci.ifEmpty { node.uci }

            val boardBefore = try {
                ChessBoardState.fromFen(node.fenBefore)
            } catch (_: Exception) {
                ChessBoardState.fromFen(ChessEngine.INITIAL_FEN)
            }
            val legalMoves = ChessEngine.generateLegalMoves(boardBefore)
            val legalMovesCount = legalMoves.size

            val playedMoveObj = legalMoves.firstOrNull { it.uci == node.uci }
            val pieceChar = playedMoveObj?.let { boardBefore.pieceAt(it.from)?.type?.code } ?: 'p'
            val capturedChar = when {
                playedMoveObj?.isEnPassant == true -> 'p'
                playedMoveObj?.capturedPiece != null -> playedMoveObj.capturedPiece.type.code
                else -> null
            }
            val promotionChar = playedMoveObj?.promotion?.code

            var bestMoveSan = topLinesBefore.firstOrNull()?.bestMoveSan ?: node.san
            if (bestMoveSan.isEmpty() && bestMoveUci.isNotEmpty()) {
                val parsedBest = ChessEngine.parseUciMove(boardBefore, bestMoveUci)
                if (parsedBest != null) {
                    bestMoveSan = ChessEngine.moveToSan(boardBefore, parsedBest)
                }
            }

            // Evaluate position AFTER the move
            val nextResult = evaluateFen(node.fenAfter)
            val evalAfter = nextResult.evaluation

            // Check Opening Book chain
            val stepInBook = EcoOpeningBook.isBookMove(node.fenBefore, node.fenAfter, node.ply)
            if (!stepInBook) {
                inBookChain = false
            }
            val isBook = inBookChain && stepInBook
            val openingEntry = EcoOpeningBook.getOpeningByFen(node.fenAfter)
                ?: EcoOpeningBook.getOpeningByFen(node.fenBefore)

            // Detect material sacrifice and piece losses via SEE
            val isSacrifice = isMaterialSacrificeMove(node.fenBefore, node.from, node.to, promotionChar)
            val bestFrom = if (bestMoveUci.length >= 2) bestMoveUci.substring(0, 2) else node.from
            val bestTo = if (bestMoveUci.length >= 4) bestMoveUci.substring(2, 4) else node.to
            val bestPromo = if (bestMoveUci.length >= 5) bestMoveUci[4] else null
            val didBestMoveLosePiece = doesMoveLosePiece(node.fenBefore, bestFrom, bestTo, bestPromo)
            val didPlayedMoveLosePiece = doesMoveLosePiece(node.fenBefore, node.from, node.to, promotionChar)

            val classified = classifyMoveFromEvaluations(
                evalBefore = evalBefore,
                evalAfter = evalAfter,
                moverColor = node.color,
                playedUci = node.uci,
                playedSan = node.san,
                bestMoveUci = bestMoveUci,
                bestMoveSan = bestMoveSan,
                isBook = isBook,
                legalMovesCount = legalMovesCount,
                topLinesBefore = topLinesBefore,
                isSacrifice = isSacrifice,
                thresholds = thresholds,
                didBestMoveLosePiece = didBestMoveLosePiece,
                didPlayedMoveLosePiece = didPlayedMoveLosePiece
            )

            val accuracy = AccuracyCalculator.calculateSingleMoveAccuracy(
                winProbLoss = classified.winProbLoss,
                isBook = isBook,
                isForced = classified.category == MoveCategory.FORCED
            )

            val moveAnalysis = MoveAnalysis(
                id = node.id,
                ply = node.ply,
                moveNumber = node.moveNumber,
                color = node.color,
                san = node.san,
                uci = node.uci,
                from = node.from,
                to = node.to,
                piece = pieceChar,
                captured = capturedChar,
                promotion = promotionChar,
                fenBefore = node.fenBefore,
                fenAfter = node.fenAfter,
                isBook = isBook,
                openingEco = openingEntry?.eco,
                openingName = openingEntry?.name,
                legalMovesCount = legalMovesCount,
                evalBefore = evalBefore,
                evalAfter = evalAfter,
                bestMoveUci = bestMoveUci,
                bestMoveSan = bestMoveSan,
                topLines = topLinesBefore,
                cpLoss = classified.cpLoss,
                winProbBefore = classified.winProbBefore,
                winProbAfter = classified.winProbAfter,
                winProbLoss = classified.winProbLoss,
                accuracy = accuracy,
                category = classified.category,
                isSacrifice = isSacrifice,
                isOnlyBestMove = classified.isOnlyBestMove,
                isMissedTactic = classified.isMissedTactic,
                explanation = classified.explanation,
                comment = node.comment
            )

            results.add(moveAnalysis)
            prevResult = nextResult
            onProgress?.invoke(i + 1, totalPlies, moveAnalysis)
        }

        val stats = AccuracyCalculator.calculateGameStatistics(results)
        return results to stats
    }

    // =========================================================================
    // Deterministic PeSTO + Alpha-Beta MultiPV Fallback (used in JVM unit tests
    // or if Android WebView is unavailable)
    // =========================================================================

    private val PAWN_PST = intArrayOf(
          0,   0,   0,   0,   0,   0,   0,   0,
         50,  50,  50,  50,  50,  50,  50,  50,
         10,  10,  20,  30,  30,  20,  10,  10,
          5,   5,  10,  27,  27,  10,   5,   5,
          0,   0,   0,  24,  24,   0,   0,   0,
          5,  -5, -10,   0,   0, -10,  -5,   5,
          5,  10,  10, -24, -24,  10,  10,   5,
          0,   0,   0,   0,   0,   0,   0,   0
    )

    private val KNIGHT_PST = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20,   0,   0,   0,   0, -20, -40,
        -30,   0,  10,  15,  15,  10,   0, -30,
        -30,   5,  15,  20,  20,  15,   5, -30,
        -30,   0,  15,  20,  20,  15,   0, -30,
        -30,   5,  10,  15,  15,  10,   5, -30,
        -40, -20,   0,   5,   5,   0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    )

    private val BISHOP_PST = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10,   0,   0,   0,   0,   0,   0, -10,
        -10,   0,   9,  10,  10,   9,   0, -10,
        -10,   5,   5,  12,  12,   5,   5, -10,
        -10,   0,  10,  12,  12,  10,   0, -10,
        -10,  10,  10,  10,  10,  10,  10, -10,
        -10,   5,   0,   0,   0,   0,   5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    )

    private val ROOK_PST = intArrayOf(
          0,   0,   0,   0,   0,   0,   0,   0,
          5,  15,  15,  15,  15,  15,  15,   5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
          0,   0,   0,   8,   8,   0,   0,   0
    )

    private val QUEEN_PST = intArrayOf(
        -20, -10, -10,  -5,  -5, -10, -10, -20,
        -10,   0,   0,   0,   0,   0,   0, -10,
        -10,   0,   5,   5,   5,   5,   0, -10,
         -5,   0,   5,   5,   5,   5,   0,  -5,
          0,   0,   5,   5,   5,   5,   0,  -5,
        -10,   5,   5,   5,   5,   5,   0, -10,
        -10,   0,   5,   0,   0,   0,   0, -10,
        -20, -10, -10,  -5,  -5, -10, -10, -20
    )

    private val KING_MID_PST = intArrayOf(
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -10, -20, -20, -20, -20, -20, -20, -10,
         20,  20,   0,   0,   0,   0,  20,  20,
         20,  30,  10,   0,   0,  10,  30,  20
    )

    fun evaluateStaticWhiteCentipawns(board: ChessBoardState): Int {
        var score = 0
        var whiteBishops = 0
        var blackBishops = 0

        for (sq in 0 until 64) {
            val piece = board.pieceAt(sq) ?: continue
            val pstIndex = if (piece.color == PieceColor.WHITE) sq else (7 - sq / 8) * 8 + (sq % 8)
            val pstBonus = when (piece.type) {
                PieceType.PAWN -> PAWN_PST[pstIndex]
                PieceType.KNIGHT -> KNIGHT_PST[pstIndex]
                PieceType.BISHOP -> BISHOP_PST[pstIndex]
                PieceType.ROOK -> ROOK_PST[pstIndex]
                PieceType.QUEEN -> QUEEN_PST[pstIndex]
                PieceType.KING -> KING_MID_PST[pstIndex]
            }
            val value = piece.type.centipawnValue + pstBonus
            if (piece.color == PieceColor.WHITE) {
                score += value
                if (piece.type == PieceType.BISHOP) whiteBishops++
            } else {
                score -= value
                if (piece.type == PieceType.BISHOP) blackBishops++
            }
        }
        if (whiteBishops >= 2) score += 30
        if (blackBishops >= 2) score -= 30
        return score
    }

    private fun minimaxWhite(
        board: ChessBoardState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int
    ): Int {
        val legalMoves = ChessEngine.generateLegalMoves(board)
        if (legalMoves.isEmpty()) {
            return if (ChessEngine.isKingInCheck(board, board.sideToMove)) {
                if (board.sideToMove == PieceColor.WHITE) -9900 - depth * 20 else 9900 + depth * 20
            } else {
                0
            }
        }
        if (depth <= 0) {
            return evaluateStaticWhiteCentipawns(board)
        }

        val ordered = legalMoves.sortedByDescending { move ->
            val captured = move.capturedPiece?.type?.centipawnValue ?: 0
            val promo = move.promotion?.centipawnValue ?: 0
            captured * 10 + promo
        }

        var alpha = alphaInit
        var beta = betaInit

        if (board.sideToMove == PieceColor.WHITE) {
            var best = -100000
            for (move in ordered) {
                val child = ChessEngine.applyMove(board, move)
                val eval = minimaxWhite(child, depth - 1, alpha, beta)
                if (eval > best) best = eval
                if (eval > alpha) alpha = eval
                if (beta <= alpha) break
            }
            return best
        } else {
            var best = 100000
            for (move in ordered) {
                val child = ChessEngine.applyMove(board, move)
                val eval = minimaxWhite(child, depth - 1, alpha, beta)
                if (eval < best) best = eval
                if (eval < beta) beta = eval
                if (beta <= alpha) break
            }
            return best
        }
    }

    fun evaluatePositionFallback(
        fen: String,
        config: EngineConfig = EngineConfig()
    ): StockfishUciBridge.StockfishAnalysisResult {
        val board = ChessBoardState.fromFen(fen)
        val legalMoves = ChessEngine.generateLegalMoves(board)
        if (legalMoves.isEmpty()) {
            val inCheck = ChessEngine.isKingInCheck(board, board.sideToMove)
            val mate = if (inCheck) {
                if (board.sideToMove == PieceColor.WHITE) -1 else 1
            } else null
            val ev = EngineEvaluation(cp = if (inCheck) null else 0, mate = mate, depth = config.depth)
            return StockfishUciBridge.StockfishAnalysisResult("", ev, emptyList())
        }

        val searchDepth = if (legalMoves.size > 28) 2 else 3
        val scored = legalMoves.map { move ->
            val after = ChessEngine.applyMove(board, move)
            val cpWhite = minimaxWhite(after, searchDepth - 1, -100000, 100000)
            val replyMoves = ChessEngine.generateLegalMoves(after)
            val bestReply = replyMoves.firstOrNull()
            val pvUci = if (bestReply != null) listOf(move.uci, bestReply.uci) else listOf(move.uci)
            val san = ChessEngine.moveToSan(board, move)
            val pvSan = if (bestReply != null) {
                listOf(san, ChessEngine.moveToSan(after, bestReply))
            } else {
                listOf(san)
            }
            Triple(move, cpWhite, pvUci to pvSan)
        }

        val sorted = if (board.sideToMove == PieceColor.WHITE) {
            scored.sortedByDescending { it.second }
        } else {
            scored.sortedBy { it.second }
        }

        val lines = sorted.take(config.multiPv.coerceIn(1, 5)).mapIndexed { idx, (m, score, pvs) ->
            val isMate = abs(score) >= 9500
            val mateVal = if (isMate) (if (score > 0) 1 else -1) else null
            EngineLine(
                multipv = idx + 1,
                depth = config.depth,
                cp = if (isMate) null else score,
                mate = mateVal,
                pvUci = pvs.first,
                pvSan = pvs.second,
                bestMoveUci = m.uci,
                bestMoveSan = pvs.second.firstOrNull() ?: m.uci
            )
        }

        val bestLine = lines.first()
        val evaluation = EngineEvaluation(
            cp = bestLine.cp,
            mate = bestLine.mate,
            depth = config.depth,
            pv = bestLine.pvUci,
            multipv = 1
        )
        return StockfishUciBridge.StockfishAnalysisResult(
            bestMoveUci = bestLine.bestMoveUci,
            evaluation = evaluation,
            lines = lines
        )
    }
}
