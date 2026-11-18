package com.nikanokh.chessinsightanalyzermobile.domain.analysis

import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineEvaluation
import com.nikanokh.chessinsightanalyzermobile.domain.model.GameStatistics
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveAnalysis
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.PlayerStatistics
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * 1:1 port of Web `src/core/analysis/AccuracyCalculator.ts`
 * Implements Lichess open-source win probability, single-move accuracy,
 * sliding-window volatility weighting, and 50/50 weighted + harmonic mean game accuracy.
 */
object AccuracyCalculator {

    /**
     * Exact Lichess open-source logistic coefficient:
     * https://github.com/lichess-org/lila/blob/master/modules/analyse/src/main/WinPercent.scala
     */
    const val WIN_PROB_LOGISTIC_COEFFICIENT = -0.00368208
    const val MAX_CLAMPED_CP = 4000

    /**
     * Converts centipawns (or mate score) from White's perspective into
     * White's winning probability [0..100].
     */
    fun cpToWhiteWinProbability(cp: Int?, mate: Int?): Double {
        if (mate != null) {
            if (mate == 0) return 50.0
            val dist = max(1, kotlin.math.abs(mate))
            val mateDiscount = min(2.4, (dist - 1) * 0.25)
            return if (mate > 0) 100.0 - mateDiscount else mateDiscount
        }
        val clampedCp = max(-MAX_CLAMPED_CP, min(MAX_CLAMPED_CP, cp ?: 0))
        val prob = 50.0 + 50.0 * (2.0 / (1.0 + exp(WIN_PROB_LOGISTIC_COEFFICIENT * clampedCp)) - 1.0)
        return prob.coerceIn(0.0, 100.0)
    }

    /**
     * Converts White's winning probability to the given player's perspective [0..100].
     */
    fun getPlayerWinProbability(whiteWinProb: Double, color: PieceColor): Double {
        return if (color == PieceColor.WHITE) whiteWinProb else 100.0 - whiteWinProb
    }

    /**
     * Computes the exact winning probability before, after, and lost by a single move
     * from the moving player's perspective.
     */
    fun computeMoveWinProbLoss(
        evalBefore: EngineEvaluation,
        evalAfter: EngineEvaluation,
        moverColor: PieceColor
    ): MoveWinProbResult {
        val whiteWinBefore = cpToWhiteWinProbability(evalBefore.cp, evalBefore.mate)
        val whiteWinAfter = cpToWhiteWinProbability(evalAfter.cp, evalAfter.mate)

        val playerWinBefore = getPlayerWinProbability(whiteWinBefore, moverColor)
        val playerWinAfter = getPlayerWinProbability(whiteWinAfter, moverColor)

        val winProbLoss = max(0.0, playerWinBefore - playerWinAfter)
        return MoveWinProbResult(
            winProbBefore = playerWinBefore,
            winProbAfter = playerWinAfter,
            winProbLoss = winProbLoss
        )
    }

    data class MoveWinProbResult(
        val winProbBefore: Double,
        val winProbAfter: Double,
        val winProbLoss: Double
    )

    /**
     * Exact Lichess open-source raw move accuracy formula from WinPercent loss:
     * https://github.com/lichess-org/lila/blob/master/modules/analyse/src/main/AccuracyPercent.scala
     * raw = 103.1668100711649 * exp(-0.04354415386753951 * winProbLoss) - 3.166924740191411
     * Clamped to [0, 100] with +1 uncertainty bonus matching Lichess AccuracyPercent.withBonus
     */
    fun calculateRawSingleMoveAccuracy(winProbLoss: Double): Double {
        if (winProbLoss <= 0.0) return 100.0
        val raw = 103.1668100711649 * exp(-0.04354415386753951 * winProbLoss) - 3.166924740191411 + 1.0
        return max(0.0, min(100.0, raw))
    }

    /**
     * Computes single-move accuracy [0..100] displayed in the UI move list.
     * Forced moves (only 1 legal move) and theoretical book moves are 100% accurate.
     */
    fun calculateSingleMoveAccuracy(
        winProbLoss: Double,
        isBook: Boolean = false,
        isForced: Boolean = false
    ): Double {
        if (isBook || isForced) return 100.0
        val acc = calculateRawSingleMoveAccuracy(winProbLoss)
        return round(acc * 10.0) / 10.0
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size <= 1) return 0.0
        val mean = values.sum() / values.size.toDouble()
        val variance = values.sumOf { (it - mean).pow(2) } / values.size.toDouble()
        return sqrt(variance)
    }

    /**
     * Estimates performance rating (ELO) from ACPL and overall accuracy.
     * Matches Web `AccuracyCalculator.ts` `estimateElo`.
     */
    fun estimateElo(acpl: Double, accuracy: Double): Int {
        val raw = 3100.0 * exp(-0.014 * acpl) * (0.55 + 0.45 * (accuracy / 100.0))
        return round(max(400.0, min(3200.0, raw))).toInt()
    }

    private fun phaseAccuracy(moves: List<MoveAnalysis>): Double {
        if (moves.isEmpty()) return 0.0
        val avg = moves.sumOf { it.accuracy } / moves.size.toDouble()
        return round(avg * 10.0) / 10.0
    }

    /**
     * Aggregates full game statistics for both White and Black using
     * Lichess sliding-window volatility weighting + harmonic mean.
     */
    fun calculateGameStatistics(moves: List<MoveAnalysis>): GameStatistics {
        // Build array of White win probabilities for every ply (starting from initial position = 50%)
        val whiteWinProbs = ArrayList<Double>(moves.size + 1)
        whiteWinProbs.add(50.0)
        for (m in moves) {
            val wAfter = if (m.color == PieceColor.WHITE) m.winProbAfter else 100.0 - m.winProbAfter
            whiteWinProbs.add(wAfter)
        }

        // Sliding window size clamped to [2, 8] matching Lichess AccuracyPercent.scala
        val windowSize = max(2, min(8, round(moves.size / 10.0).toInt()))
        val plyWeights = DoubleArray(moves.size)
        for (i in moves.indices) {
            val startIdx = max(0, i + 1 - windowSize)
            val endIdx = min(whiteWinProbs.size, startIdx + windowSize)
            val slice = whiteWinProbs.subList(startIdx, min(whiteWinProbs.size, max(startIdx + 2, endIdx)))
            val rawStdDev = standardDeviation(slice)
            plyWeights[i] = max(0.5, min(12.0, rawStdDev))
        }

        fun buildPlayerStats(color: PieceColor): PlayerStatistics {
            val playerMoves = moves.filter { it.color == color }
            val categoryCounts = MoveCategory.entries.associateWith { 0 }.toMutableMap()

            if (playerMoves.isEmpty()) {
                return PlayerStatistics(
                    accuracy = 0.0,
                    averageCentipawnLoss = 0.0,
                    estimatedElo = 1200,
                    totalMoves = 0,
                    categoryCounts = categoryCounts
                )
            }

            var totalCpl = 0.0
            var cplMovesCount = 0
            var weightedAccSum = 0.0
            var weightSum = 0.0
            val moveAccuracies = ArrayList<Double>(playerMoves.size)

            for (i in moves.indices) {
                val m = moves[i]
                if (m.color != color) continue

                categoryCounts[m.category] = (categoryCounts[m.category] ?: 0) + 1

                if (!m.isBook) {
                    totalCpl += min(m.cpLoss, 500)
                    cplMovesCount++
                }

                val moveAcc = m.accuracy
                val weight = plyWeights[i]
                weightedAccSum += moveAcc * weight
                weightSum += weight
                moveAccuracies.add(max(1.0, moveAcc)) // clamp to >=1 to avoid division by zero in harmonic mean
            }

            val weightedMean = if (weightSum > 0.0) weightedAccSum / weightSum else 100.0
            val harmonicDenominator = moveAccuracies.sumOf { 1.0 / it }
            val harmonicMean = if (harmonicDenominator > 0.0) {
                moveAccuracies.size.toDouble() / harmonicDenominator
            } else {
                100.0
            }

            val finalAccuracy = round(((weightedMean + harmonicMean) / 2.0) * 10.0) / 10.0
            val acpl = if (cplMovesCount > 0) {
                round((totalCpl / cplMovesCount.toDouble()) * 10.0) / 10.0
            } else {
                0.0
            }

            return PlayerStatistics(
                accuracy = max(0.0, min(100.0, finalAccuracy)),
                averageCentipawnLoss = acpl,
                estimatedElo = estimateElo(acpl, finalAccuracy),
                totalMoves = playerMoves.size,
                categoryCounts = categoryCounts
            )
        }

        val whiteMoves = moves.filter { it.color == PieceColor.WHITE }
        val blackMoves = moves.filter { it.color == PieceColor.BLACK }

        return GameStatistics(
            white = buildPlayerStats(PieceColor.WHITE),
            black = buildPlayerStats(PieceColor.BLACK),
            openingAccuracyWhite = phaseAccuracy(whiteMoves.filter { it.moveNumber <= 12 }),
            middlegameAccuracyWhite = phaseAccuracy(whiteMoves.filter { it.moveNumber in 13..28 }),
            endgameAccuracyWhite = phaseAccuracy(whiteMoves.filter { it.moveNumber > 28 }),
            openingAccuracyBlack = phaseAccuracy(blackMoves.filter { it.moveNumber <= 12 }),
            middlegameAccuracyBlack = phaseAccuracy(blackMoves.filter { it.moveNumber in 13..28 }),
            endgameAccuracyBlack = phaseAccuracy(blackMoves.filter { it.moveNumber > 28 })
        )
    }
}
