package com.nikanokh.chessinsightanalyzermobile.domain.engine

import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessPosition
import com.nikanokh.chessinsightanalyzermobile.domain.model.*
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Native Kotlin Alpha-Beta + Piece-Square-Table Tactical Chess Analyzer.
 * Evaluates positions from White's perspective in centipawns (cp), identifies best moves,
 * classifies move accuracy (Brilliant, Great, Best, Book, Good, Inaccuracy, Mistake, Blunder),
 * and calculates full-game accuracy percentages.
 */
object ChessAnalyzerEngine {

    private val PAWN_PST = arrayOf(
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5,  5, 10, 28, 28, 10,  5,  5),
        intArrayOf(0,  0,  0, 24, 24,  0,  0,  0),
        intArrayOf(5, -5,-10,  0,  0,-10, -5,  5),
        intArrayOf(5, 10, 10,-24,-24, 10, 10,  5),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val KNIGHT_PST = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 12, 16, 16, 12,  0,-30),
        intArrayOf(-30,  6, 16, 22, 22, 16,  6,-30),
        intArrayOf(-30,  0, 16, 22, 22, 16,  0,-30),
        intArrayOf(-30,  6, 12, 16, 16, 12,  6,-30),
        intArrayOf(-40,-20,  0,  6,  6,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )

    private val BISHOP_PST = arrayOf(
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  8, 12, 12,  8,  0,-10),
        intArrayOf(-10,  6,  6, 12, 12,  6,  6,-10),
        intArrayOf(-10,  0, 12, 12, 12, 12,  0,-10),
        intArrayOf(-10, 12, 12, 12, 12, 12, 12,-10),
        intArrayOf(-10,  6,  0,  0,  0,  0,  6,-10),
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20)
    )

    fun evaluateStaticWhitePov(pos: ChessPosition): Int {
        var score = 0
        for (f in 0..7) {
            for (r in 0..7) {
                val p = pos.board[f][r] ?: continue
                val baseCp = when (p.type) {
                    PieceType.PAWN -> 100
                    PieceType.KNIGHT -> 320
                    PieceType.BISHOP -> 330
                    PieceType.ROOK -> 500
                    PieceType.QUEEN -> 900
                    PieceType.KING -> 0
                }
                val tableRank = if (p.color == PieceColor.WHITE) 7 - r else r
                val positional = when (p.type) {
                    PieceType.PAWN -> PAWN_PST[tableRank][f]
                    PieceType.KNIGHT -> KNIGHT_PST[tableRank][f]
                    PieceType.BISHOP -> BISHOP_PST[tableRank][f]
                    PieceType.ROOK -> if (tableRank == 1) 25 else 0
                    PieceType.QUEEN -> (3 - kotlin.math.abs(3 - f)) * 3
                    PieceType.KING -> if (f == 6 || f == 2) 25 else -10
                }
                if (p.color == PieceColor.WHITE) {
                    score += baseCp + positional
                } else {
                    score -= (baseCp + positional)
                }
            }
        }
        return score
    }

    private fun alphaBetaWhitePov(
        pos: ChessPosition,
        depth: Int,
        alphaInit: Int,
        betaInit: Int
    ): Int {
        val legal = pos.generateLegalMoves()
        if (legal.isEmpty()) {
            return if (pos.isInCheck(pos.sideToMove)) {
                if (pos.sideToMove == PieceColor.WHITE) -29000 - depth * 100 else 29000 + depth * 100
            } else {
                0
            }
        }
        if (depth <= 0) {
            return evaluateStaticWhitePov(pos)
        }

        // Order captures and promotions first for faster alpha-beta pruning
        val ordered = legal.sortedByDescending {
            (it.captured?.type?.materialValue ?: 0) * 10 + (if (it.promotion != null) 80 else 0)
        }

        var alpha = alphaInit
        var beta = betaInit

        if (pos.sideToMove == PieceColor.WHITE) {
            var best = -30000
            for (m in ordered) {
                val score = alphaBetaWhitePov(pos.applyMove(m), depth - 1, alpha, beta)
                best = max(best, score)
                alpha = max(alpha, best)
                if (beta <= alpha) break
            }
            return best
        } else {
            var best = 30000
            for (m in ordered) {
                val score = alphaBetaWhitePov(pos.applyMove(m), depth - 1, alpha, beta)
                best = min(best, score)
                beta = min(beta, best)
                if (beta <= alpha) break
            }
            return best
        }
    }

    data class PositionSearchResult(
        val evalWhiteCp: Int,
        val mateIn: Int?,
        val bestMoveUci: String,
        val bestMoveSan: String
    )

    fun analyzePosition(pos: ChessPosition, configuredDepth: Int = 14): PositionSearchResult {
        val searchDepth = when {
            configuredDepth >= 18 -> 3
            configuredDepth >= 14 -> 2
            else -> 2
        }
        val legal = pos.generateLegalMoves()
        if (legal.isEmpty()) {
            val isCheck = pos.isInCheck(pos.sideToMove)
            val eval = if (!isCheck) 0 else if (pos.sideToMove == PieceColor.WHITE) -29900 else 29900
            val mate = if (!isCheck) null else if (pos.sideToMove == PieceColor.WHITE) -1 else 1
            return PositionSearchResult(eval, mate, "", "")
        }

        var bestMove = legal.first()
        var bestScore = if (pos.sideToMove == PieceColor.WHITE) -30000 else 30000

        for (m in legal) {
            val nextPos = pos.applyMove(m)
            val score = alphaBetaWhitePov(nextPos, searchDepth - 1, -30000, 30000)
            if (pos.sideToMove == PieceColor.WHITE) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = m
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = m
                }
            }
        }

        val mate = when {
            bestScore >= 28000 -> 1
            bestScore <= -28000 -> -1
            else -> null
        }

        return PositionSearchResult(
            evalWhiteCp = bestScore.coerceIn(-2500, 2500),
            mateIn = mate,
            bestMoveUci = bestMove.uci,
            bestMoveSan = pos.toSan(bestMove, legal)
        )
    }

    fun classifyMove(
        ply: Int,
        moverColor: PieceColor,
        playedUci: String,
        bestMoveUci: String,
        evalBeforeWhiteCp: Int,
        evalAfterWhiteCp: Int
    ): Pair<Int, MoveCategory> {
        val cpl = if (moverColor == PieceColor.WHITE) {
            (evalBeforeWhiteCp - evalAfterWhiteCp).coerceAtLeast(0)
        } else {
            (evalAfterWhiteCp - evalBeforeWhiteCp).coerceAtLeast(0)
        }

        val category = when {
            ply <= 6 && cpl <= 30 -> MoveCategory.BOOK
            playedUci == bestMoveUci && cpl == 0 && ply > 8 && kotlin.math.abs(evalAfterWhiteCp) > 150 ->
                MoveCategory.GREAT
            playedUci == bestMoveUci || cpl <= 12 -> MoveCategory.BEST
            cpl <= 35 -> MoveCategory.GOOD
            cpl <= 85 -> MoveCategory.INACCURACY
            cpl <= 190 -> MoveCategory.MISTAKE
            else -> MoveCategory.BLUNDER
        }
        return cpl to category
    }

    suspend fun analyzeFullGame(
        moves: List<MoveNode>,
        configuredDepth: Int,
        onProgress: (current: Int, total: Int) -> Unit
    ): Pair<List<MoveNode>, GameStatistics> {
        val total = moves.size
        if (total == 0) {
            return emptyList<MoveNode>() to GameStatistics(
                whiteAccuracy = 100.0,
                blackAccuracy = 100.0,
                whiteAvgCpl = 0,
                blackAvgCpl = 0,
                whiteCategoryCounts = emptyMap(),
                blackCategoryCounts = emptyMap()
            )
        }

        val analyzedMoves = mutableListOf<MoveNode>()
        val whiteCpls = mutableListOf<Int>()
        val blackCpls = mutableListOf<Int>()
        val whiteCounts = mutableMapOf<MoveCategory, Int>()
        val blackCounts = mutableMapOf<MoveCategory, Int>()

        var prevSearch = analyzePosition(ChessPosition.fromFen(moves.first().fenBefore), configuredDepth)

        for ((index, node) in moves.withIndex()) {
            coroutineContext.ensureActive()
            onProgress(index + 1, total)

            val afterPos = ChessPosition.fromFen(node.fenAfter)
            val afterSearch = analyzePosition(afterPos, configuredDepth)

            val (cpl, category) = classifyMove(
                ply = node.ply,
                moverColor = node.color,
                playedUci = node.uci,
                bestMoveUci = prevSearch.bestMoveUci,
                evalBeforeWhiteCp = prevSearch.evalWhiteCp,
                evalAfterWhiteCp = afterSearch.evalWhiteCp
            )

            if (node.color == PieceColor.WHITE) {
                whiteCpls.add(cpl)
                whiteCounts[category] = (whiteCounts[category] ?: 0) + 1
            } else {
                blackCpls.add(cpl)
                blackCounts[category] = (blackCounts[category] ?: 0) + 1
            }

            val moveAnalysis = MoveAnalysis(
                evalBeforeCp = prevSearch.evalWhiteCp,
                evalAfterCp = afterSearch.evalWhiteCp,
                mateAfter = afterSearch.mateIn,
                cpl = cpl,
                bestMoveUci = prevSearch.bestMoveUci,
                bestMoveSan = prevSearch.bestMoveSan,
                category = category
            )

            analyzedMoves.add(node.copy(analysis = moveAnalysis))
            prevSearch = afterSearch
        }

        fun computeAccuracy(cpls: List<Int>): Double {
            if (cpls.isEmpty()) return 100.0
            val avg = cpls.average()
            val raw = 103.1668 * exp(-0.004 * avg) - 3.1669
            return ((raw.coerceIn(15.0, 99.8) * 10.0).roundToInt()) / 10.0
        }

        val stats = GameStatistics(
            whiteAccuracy = computeAccuracy(whiteCpls),
            blackAccuracy = computeAccuracy(blackCpls),
            whiteAvgCpl = if (whiteCpls.isEmpty()) 0 else whiteCpls.average().roundToInt(),
            blackAvgCpl = if (blackCpls.isEmpty()) 0 else blackCpls.average().roundToInt(),
            whiteCategoryCounts = whiteCounts,
            blackCategoryCounts = blackCounts
        )

        return analyzedMoves to stats
    }
}
