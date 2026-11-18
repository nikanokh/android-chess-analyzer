package com.nikanokh.chessinsightanalyzermobile

import com.nikanokh.chessinsightanalyzermobile.data.remote.ChessComGameItem
import com.nikanokh.chessinsightanalyzermobile.data.remote.ChessComPlayerInfo
import com.nikanokh.chessinsightanalyzermobile.data.remote.OnlineImportService
import com.nikanokh.chessinsightanalyzermobile.domain.analysis.AccuracyCalculator
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessBoardState
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessEngine
import com.nikanokh.chessinsightanalyzermobile.domain.chess.EcoOpeningBook
import com.nikanokh.chessinsightanalyzermobile.domain.engine.ChessAnalyzerEngine
import com.nikanokh.chessinsightanalyzermobile.domain.model.ClassificationThresholds
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineEvaluation
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineLine
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveAnalysis
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParityTest {

    @Test
    fun testInitialPositionAndOperaGamePgn() {
        val start = ChessBoardState.fromFen(ChessEngine.INITIAL_FEN)
        val legal = ChessEngine.generateLegalMoves(start)
        assertEquals(20, legal.size)

        val operaPgn = OnlineImportService.SAMPLE_GAMES.first().pgn
        val parsed = ChessEngine.parsePgn(operaPgn)
        assertEquals(33, parsed.nodes.size)
        assertEquals("Rd8#", parsed.nodes.last().san)

        val finalBoard = ChessBoardState.fromFen(parsed.nodes.last().fenAfter)
        assertTrue(ChessEngine.isKingInCheck(finalBoard, PieceColor.BLACK))
        assertEquals(0, ChessEngine.generateLegalMoves(finalBoard).size)
    }

    @Test
    fun testOpeningBook42EntriesAndDeviation() {
        assertTrue(EcoOpeningBook.INITIAL_ECO_ENTRIES.size >= 42)

        val parsed = ChessEngine.parsePgn("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. a4")
        val inputs = parsed.nodes.map {
            EcoOpeningBook.MainlineMoveInput(
                ply = it.ply,
                moveNumber = it.moveNumber,
                color = it.color,
                san = it.san,
                uci = it.uci,
                fenBefore = it.fenBefore,
                fenAfter = it.fenAfter
            )
        }
        val summary = EcoOpeningBook.analyzeMainlineOpening(inputs)
        assertEquals("C53", summary.eco)
        assertEquals("Итальянская партия", summary.name)
        assertEquals("Тихая игра (Giuoco Piano)", summary.variation)
        assertEquals(6, summary.lastBookPly)
        assertNotNull(summary.deviation)
        assertEquals(7, summary.deviation?.ply)
        assertEquals("a4", summary.deviation?.playedSan)
    }

    @Test
    fun testAccuracyCalculatorExactLichessFormulas() {
        // Equal position (0 cp) -> 50.0% win probability
        val wp0 = AccuracyCalculator.cpToWhiteWinProbability(0, null)
        assertEquals(50.0, wp0, 1e-6)

        // Mate for White -> 100% (M1), 99.5% (M3); Mate for Black -> 0.25% (-M2)
        assertEquals(100.0, AccuracyCalculator.cpToWhiteWinProbability(null, 1), 1e-6)
        assertEquals(99.5, AccuracyCalculator.cpToWhiteWinProbability(null, 3), 1e-6)
        assertEquals(0.25, AccuracyCalculator.cpToWhiteWinProbability(null, -2), 1e-6)

        // Zero loss -> 100% single move accuracy
        assertEquals(100.0, AccuracyCalculator.calculateSingleMoveAccuracy(0.0), 1e-6)

        // Book & Forced moves -> 100%
        assertEquals(100.0, AccuracyCalculator.calculateSingleMoveAccuracy(25.0, isBook = true), 1e-6)
        assertEquals(100.0, AccuracyCalculator.calculateSingleMoveAccuracy(25.0, isForced = true), 1e-6)

        // Exact Lichess curve check at winProbLoss = 10.0:
        // raw = 103.1668100711649 * exp(-0.04354415386753951 * 10) - 3.166924740191411 + 1 = 64.59 -> 64.6
        val acc10 = AccuracyCalculator.calculateSingleMoveAccuracy(10.0)
        assertEquals(64.6, acc10, 0.1)
    }

    @Test
    fun testAll11MoveCategoriesAndSeeSacrifice() {
        assertEquals(11, MoveCategory.entries.size)

        // 1. Book move
        val bookRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 20),
            evalAfter = EngineEvaluation(cp = 20),
            moverColor = PieceColor.WHITE,
            playedUci = "e2e4",
            playedSan = "e4",
            bestMoveUci = "e2e4",
            bestMoveSan = "e4",
            isBook = true,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.BOOK, bookRes.category)

        // 2. Forced move (legalMovesCount = 1)
        val forcedRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = -100),
            evalAfter = EngineEvaluation(cp = -120),
            moverColor = PieceColor.WHITE,
            playedUci = "g1h1",
            playedSan = "Kh1",
            bestMoveUci = "g1h1",
            bestMoveSan = "Kh1",
            isBook = false,
            legalMovesCount = 1,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.FORCED, forcedRes.category)

        // 3. Brilliant move (isSacrifice = true, best move played, strong position)
        val brilliantRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 150),
            evalAfter = EngineEvaluation(cp = 160),
            moverColor = PieceColor.WHITE,
            playedUci = "f3f7",
            playedSan = "Nxf7",
            bestMoveUci = "f3f7",
            bestMoveSan = "Nxf7",
            isBook = false,
            legalMovesCount = 25,
            topLinesBefore = emptyList(),
            isSacrifice = true
        )
        assertEquals(MoveCategory.BRILLIANT, brilliantRes.category)

        // 4. Great move (only best move via MultiPV gap >= 14% win prob)
        val topLinesForGreat = listOf(
            EngineLine(1, 18, 220, null, listOf("d1d8"), listOf("Qxd8+"), "d1d8", "Qxd8+"),
            EngineLine(2, 18, -50, null, listOf("a2a3"), listOf("a3"), "a2a3", "a3")
        )
        val greatRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 220),
            evalAfter = EngineEvaluation(cp = 220),
            moverColor = PieceColor.WHITE,
            playedUci = "d1d8",
            playedSan = "Qxd8+",
            bestMoveUci = "d1d8",
            bestMoveSan = "Qxd8+",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = topLinesForGreat,
            isSacrifice = false
        )
        assertEquals(MoveCategory.GREAT, greatRes.category)

        // 5. Best move
        val bestRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 30),
            evalAfter = EngineEvaluation(cp = 30),
            moverColor = PieceColor.WHITE,
            playedUci = "g1f3",
            playedSan = "Nf3",
            bestMoveUci = "g1f3",
            bestMoveSan = "Nf3",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.BEST, bestRes.category)

        // 6. Excellent move (~30 cp loss)
        val excellentRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 40),
            evalAfter = EngineEvaluation(cp = 10),
            moverColor = PieceColor.WHITE,
            playedUci = "d2d3",
            playedSan = "d3",
            bestMoveUci = "d2d4",
            bestMoveSan = "d4",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.EXCELLENT, excellentRes.category)

        // 7. Good move (~70 cp loss)
        val goodRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 50),
            evalAfter = EngineEvaluation(cp = -20),
            moverColor = PieceColor.WHITE,
            playedUci = "a2a3",
            playedSan = "a3",
            bestMoveUci = "d2d4",
            bestMoveSan = "d4",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.GOOD, goodRes.category)

        // 8. Inaccuracy (~130 cp loss)
        val inaccuracyRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 50),
            evalAfter = EngineEvaluation(cp = -80),
            moverColor = PieceColor.WHITE,
            playedUci = "h2h4",
            playedSan = "h4",
            bestMoveUci = "d2d4",
            bestMoveSan = "d4",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.INACCURACY, inaccuracyRes.category)

        // 9. Mistake (~220 cp loss)
        val mistakeRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 50),
            evalAfter = EngineEvaluation(cp = -170),
            moverColor = PieceColor.WHITE,
            playedUci = "g2g4",
            playedSan = "g4",
            bestMoveUci = "d2d4",
            bestMoveSan = "d4",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.MISTAKE, mistakeRes.category)

        // 10. Missed Chance (had forced mate in 3, played move dropping to +150 cp)
        val missedChanceRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = null, mate = 3),
            evalAfter = EngineEvaluation(cp = 150, mate = null),
            moverColor = PieceColor.WHITE,
            playedUci = "h2h3",
            playedSan = "h3",
            bestMoveUci = "d1h5",
            bestMoveSan = "Qh5#",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.MISSED_CHANCE, missedChanceRes.category)

        // 11. Blunder (~450 cp loss)
        val blunderRes = ChessAnalyzerEngine.classifyMoveFromEvaluations(
            evalBefore = EngineEvaluation(cp = 50),
            evalAfter = EngineEvaluation(cp = -400),
            moverColor = PieceColor.WHITE,
            playedUci = "d1e2",
            playedSan = "Qe2",
            bestMoveUci = "d2d4",
            bestMoveSan = "d4",
            isBook = false,
            legalMovesCount = 20,
            topLinesBefore = emptyList(),
            isSacrifice = false
        )
        assertEquals(MoveCategory.BLUNDER, blunderRes.category)
    }

    @Test
    fun testThresholdReclassification() {
        val sampleMove = MoveAnalysis(
            id = "ply_10",
            ply = 10,
            moveNumber = 5,
            color = PieceColor.WHITE,
            san = "a3",
            uci = "a2a3",
            from = "a2",
            to = "a3",
            piece = 'p',
            fenBefore = ChessEngine.INITIAL_FEN,
            fenAfter = ChessEngine.INITIAL_FEN,
            isBook = false,
            legalMovesCount = 20,
            evalBefore = EngineEvaluation(cp = 40),
            evalAfter = EngineEvaluation(cp = 5),
            bestMoveUci = "d2d4",
            bestMoveSan = "d4"
        )

        // With default thresholds (bestMaxCpl=20, excellentMaxCpl=50), 35 cp loss is EXCELLENT
        val defaultClassified = ChessAnalyzerEngine.reclassifyMoveAnalyses(
            listOf(sampleMove),
            ChessAnalyzerEngine.DEFAULT_THRESHOLDS
        ).first()
        assertEquals(MoveCategory.EXCELLENT, defaultClassified.category)

        // With stricter thresholds (bestMaxCpl=10, excellentMaxCpl=20, goodMaxCpl=95), 35 cp loss becomes GOOD
        val strictThresholds = ClassificationThresholds(
            bestMaxCpl = 10,
            excellentMaxCpl = 20,
            goodMaxCpl = 95,
            inaccuracyMaxCpl = 175,
            mistakeMaxCpl = 300
        )
        val strictClassified = ChessAnalyzerEngine.reclassifyMoveAnalyses(
            listOf(sampleMove),
            strictThresholds
        ).first()
        assertEquals(MoveCategory.GOOD, strictClassified.category)
    }

    @Test
    fun testChessComDeduplicationAndResultMapping() {
        assertEquals("win", OnlineImportService.mapChessComResult("win").outcome)
        assertEquals("draw", OnlineImportService.mapChessComResult("repetition").outcome)
        assertEquals("draw", OnlineImportService.mapChessComResult("stalemate").outcome)
        assertEquals("draw", OnlineImportService.mapChessComResult("timevsinsufficient").outcome)
        assertEquals("loss", OnlineImportService.mapChessComResult("checkmated").outcome)
        assertEquals("loss", OnlineImportService.mapChessComResult("timeout").outcome)
        assertEquals("loss", OnlineImportService.mapChessComResult("resigned").outcome)

        val g1 = ChessComGameItem(
            url = "https://www.chess.com/game/live/100",
            pgn = "1. e4 e5",
            timeControl = "180",
            endTime = 1700000100L,
            rated = true,
            timeClass = "blitz",
            rules = "chess",
            white = ChessComPlayerInfo("Hikaru", 3200, "win"),
            black = ChessComPlayerInfo("Player2", 2900, "resigned"),
            uuid = "uuid-100"
        )
        val g1Duplicate = g1.copy()
        val g2 = g1.copy(
            url = "https://www.chess.com/game/live/200",
            endTime = 1700000200L,
            uuid = "uuid-200"
        )

        val merged = OnlineImportService.mergeAndDeduplicateChessComGames(listOf(g1), listOf(g1Duplicate, g2))
        assertEquals(2, merged.size)
        assertEquals("uuid-200", merged[0].uuid) // newest first
        assertEquals("uuid-100", merged[1].uuid)
    }
}
