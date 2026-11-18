package com.nikanokh.chessinsightanalyzermobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikanokh.chessinsightanalyzermobile.data.local.ChessSqliteDatabase
import com.nikanokh.chessinsightanalyzermobile.data.remote.ChessComGameItem
import com.nikanokh.chessinsightanalyzermobile.data.remote.ChessComProfile
import com.nikanokh.chessinsightanalyzermobile.data.remote.OnlineImportService
import com.nikanokh.chessinsightanalyzermobile.domain.analysis.AccuracyCalculator
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessBoardState
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessEngine
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessMove
import com.nikanokh.chessinsightanalyzermobile.domain.chess.EcoOpeningBook
import com.nikanokh.chessinsightanalyzermobile.domain.engine.ChessAnalyzerEngine
import com.nikanokh.chessinsightanalyzermobile.domain.engine.StockfishUciBridge
import com.nikanokh.chessinsightanalyzermobile.domain.model.AppSettings
import com.nikanokh.chessinsightanalyzermobile.domain.model.BOARD_THEME_PRESETS
import com.nikanokh.chessinsightanalyzermobile.domain.model.ClassificationThresholds
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineConfig
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineEvaluation
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineLine
import com.nikanokh.chessinsightanalyzermobile.domain.model.GameHeaders
import com.nikanokh.chessinsightanalyzermobile.domain.model.GameStatistics
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveAnalysis
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveTreeNode
import com.nikanokh.chessinsightanalyzermobile.domain.model.OpeningAnalysisSummary
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceType
import com.nikanokh.chessinsightanalyzermobile.domain.model.RepertoireEntry
import com.nikanokh.chessinsightanalyzermobile.domain.model.SavedGameRecord
import com.nikanokh.chessinsightanalyzermobile.domain.model.SquareHighlight
import com.nikanokh.chessinsightanalyzermobile.domain.model.UserArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val INITIAL_ARCHIVE_BATCH_SIZE = 12
private const val LOAD_MORE_ARCHIVE_BATCH_SIZE = 12

data class ChessUiState(
    val headers: GameHeaders = GameHeaders(),
    val initialFen: String = ChessEngine.INITIAL_FEN,
    val currentFen: String = ChessEngine.INITIAL_FEN,
    val boardState: ChessBoardState = ChessEngine.parseFen(ChessEngine.INITIAL_FEN),
    val legalMoves: List<ChessMove> = ChessEngine.generateLegalMoves(ChessEngine.parseFen(ChessEngine.INITIAL_FEN)),
    val moveNodes: List<MoveTreeNode> = emptyList(),
    val currentPly: Int = 0, // 0 = initial position, 1..N = after move currentPly
    val selectedSquare: Int? = null,
    val pendingPromotionMove: Pair<Int, Int>? = null, // fromSq to toSq when pawn reaches 8th/1st rank
    val isFlipped: Boolean = false,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val analysisPlyText: String = "",
    val currentEvaluation: EngineEvaluation = EngineEvaluation(),
    val currentTopLines: List<EngineLine> = emptyList(),
    val analyses: List<MoveAnalysis> = emptyList(),
    val gameStats: GameStatistics = GameStatistics(),
    val openingSummary: OpeningAnalysisSummary = OpeningAnalysisSummary(),
    val userArrows: List<UserArrow> = emptyList(),
    val squareHighlights: List<SquareHighlight> = emptyList(),
    val annotationMode: Boolean = false, // when true, tapping/dragging draws highlights & user arrows
    val annotationArrowStartSq: Int? = null,
    val savedGames: List<SavedGameRecord> = emptyList(),
    val repertoire: List<RepertoireEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    // Online / Chess.com browser state (matching Web ChessComModal.tsx)
    val isImportingOnline: Boolean = false,
    val onlineImportMessage: String? = null,
    val chessComUsername: String = "",
    val chessComProfile: ChessComProfile? = null,
    val chessComArchiveUrls: List<String> = emptyList(),
    val chessComLoadedArchiveCount: Int = 0,
    val chessComSelectedArchiveUrl: String = "all_loaded",
    val chessComGames: List<ChessComGameItem> = emptyList(),
    val chessComLoading: Boolean = false,
    val chessComLoadingMore: Boolean = false,
    val chessComError: String? = null,
    val chessComResultFilter: String = "all",    // "all" | "win" | "draw" | "loss"
    val chessComColorFilter: String = "all",     // "all" | "w" | "b"
    val chessComTimeClassFilter: String = "all", // "all" | "bullet" | "blitz" | "rapid" | "daily"
    // Repertoire drill state
    val activePracticeEntry: RepertoireEntry? = null,
    val practiceStepIndex: Int = 0,
    val practiceFeedback: String? = null
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChessSqliteDatabase(application.applicationContext)
    private val stockfishBridge = StockfishUciBridge(application.applicationContext)

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null
    private var liveEvalJob: Job? = null

    init {
        val loadedSettings = db.loadSettings()
        val games = db.getAllSavedGames()
        val rep = db.getAllRepertoire()

        _uiState.update {
            it.copy(
                settings = loadedSettings,
                savedGames = games,
                repertoire = rep
            )
        }

        // Load the first sample game on startup and analyze it
        val initialPgn = OnlineImportService.SAMPLE_GAMES.first().pgn
        loadPgnGame(initialPgn, autoAnalyze = true)
    }

    private fun computeOpeningSummary(
        nodes: List<MoveTreeNode>,
        headers: GameHeaders
    ): OpeningAnalysisSummary {
        val inputs = nodes.map {
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
        return EcoOpeningBook.analyzeMainlineOpening(
            moves = inputs,
            fallbackEco = headers.eco,
            fallbackName = headers.opening
        )
    }

    fun loadPgnGame(
        pgnText: String,
        autoAnalyze: Boolean = true,
        preloadedAnalyses: List<MoveAnalysis> = emptyList()
    ) {
        analysisJob?.cancel()
        val parsed = ChessEngine.parsePgn(pgnText)
        val startBoard = ChessEngine.parseFen(parsed.initialFen)
        val legal = ChessEngine.generateLegalMoves(startBoard)
        val openingSummary = computeOpeningSummary(parsed.nodes, parsed.headers)
        val preStats = if (preloadedAnalyses.isNotEmpty()) {
            AccuracyCalculator.calculateGameStatistics(preloadedAnalyses)
        } else {
            GameStatistics()
        }
        val nodesWithAnalyses = if (preloadedAnalyses.size == parsed.nodes.size) {
            parsed.nodes.mapIndexed { idx, node -> node.copy(analysis = preloadedAnalyses[idx]) }
        } else {
            parsed.nodes
        }

        _uiState.update {
            it.copy(
                headers = parsed.headers.copy(
                    eco = openingSummary.eco,
                    opening = if (openingSummary.variation != null) {
                        "${openingSummary.name}: ${openingSummary.variation}"
                    } else {
                        openingSummary.name
                    }
                ),
                initialFen = parsed.initialFen,
                currentFen = parsed.initialFen,
                boardState = startBoard,
                legalMoves = legal,
                moveNodes = nodesWithAnalyses,
                currentPly = 0,
                selectedSquare = null,
                pendingPromotionMove = null,
                analyses = preloadedAnalyses,
                gameStats = preStats,
                openingSummary = openingSummary,
                userArrows = emptyList(),
                squareHighlights = emptyList(),
                annotationArrowStartSq = null
            )
        }

        evaluateCurrentPositionAsync(parsed.initialFen)

        if (autoAnalyze && preloadedAnalyses.isEmpty() && parsed.nodes.isNotEmpty()) {
            runFullGameAnalysis()
        }
    }

    fun loadFenPosition(fenText: String) {
        analysisJob?.cancel()
        val board = ChessEngine.parseFen(fenText)
        val canonicalFen = board.toFen()
        val legal = ChessEngine.generateLegalMoves(board)

        _uiState.update {
            it.copy(
                headers = GameHeaders(
                    event = "Позиция из FEN",
                    white = "Белые",
                    black = "Чёрные",
                    eco = "A00",
                    opening = "Пользовательская позиция"
                ),
                initialFen = canonicalFen,
                currentFen = canonicalFen,
                boardState = board,
                legalMoves = legal,
                moveNodes = emptyList(),
                currentPly = 0,
                selectedSquare = null,
                pendingPromotionMove = null,
                analyses = emptyList(),
                gameStats = GameStatistics(),
                openingSummary = OpeningAnalysisSummary(),
                userArrows = emptyList(),
                squareHighlights = emptyList()
            )
        }
        evaluateCurrentPositionAsync(canonicalFen)
    }

    fun resetToStartingBoard() {
        loadFenPosition(ChessEngine.INITIAL_FEN)
    }

    fun onSquareClicked(square: Int) {
        val state = _uiState.value

        // If annotation mode is active, tapping toggles square highlight or draws a user arrow
        if (state.annotationMode) {
            val startSq = state.annotationArrowStartSq
            if (startSq == null) {
                _uiState.update { it.copy(annotationArrowStartSq = square) }
            } else if (startSq == square) {
                toggleSquareHighlight(ChessEngine.squareToAlgebraic(square))
                _uiState.update { it.copy(annotationArrowStartSq = null) }
            } else {
                toggleUserArrow(
                    from = ChessEngine.squareToAlgebraic(startSq),
                    to = ChessEngine.squareToAlgebraic(square)
                )
                _uiState.update { it.copy(annotationArrowStartSq = null) }
            }
            return
        }

        val currentSelected = state.selectedSquare
        if (currentSelected != null) {
            val candidateMoves = state.legalMoves.filter {
                it.from == currentSelected && it.to == square
            }
            if (candidateMoves.isNotEmpty()) {
                // Check if this is a pawn promotion (multiple promotion choices)
                if (candidateMoves.size > 1 && candidateMoves.any { it.promotion != null }) {
                    _uiState.update {
                        it.copy(
                            selectedSquare = null,
                            pendingPromotionMove = currentSelected to square
                        )
                    }
                    return
                }
                makeMoveOnBoard(candidateMoves.first())
                return
            }
        }

        val piece = state.boardState.pieceAt(square)
        if (piece != null && piece.color == state.boardState.sideToMove) {
            _uiState.update {
                it.copy(selectedSquare = if (currentSelected == square) null else square)
            }
        } else {
            _uiState.update { it.copy(selectedSquare = null) }
        }
    }

    fun onPieceDraggedMove(fromSquare: Int, toSquare: Int) {
        val state = _uiState.value
        if (fromSquare == toSquare) return

        if (state.annotationMode) {
            toggleUserArrow(
                from = ChessEngine.squareToAlgebraic(fromSquare),
                to = ChessEngine.squareToAlgebraic(toSquare)
            )
            return
        }

        val candidateMoves = state.legalMoves.filter {
            it.from == fromSquare && it.to == toSquare
        }
        if (candidateMoves.isEmpty()) return

        if (candidateMoves.size > 1 && candidateMoves.any { it.promotion != null }) {
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    pendingPromotionMove = fromSquare to toSquare
                )
            }
            return
        }
        makeMoveOnBoard(candidateMoves.first())
    }

    fun completePromotion(pieceType: PieceType) {
        val state = _uiState.value
        val pending = state.pendingPromotionMove ?: return
        val move = state.legalMoves.firstOrNull {
            it.from == pending.first && it.to == pending.second && it.promotion == pieceType
        } ?: state.legalMoves.firstOrNull {
            it.from == pending.first && it.to == pending.second
        }
        _uiState.update { it.copy(pendingPromotionMove = null, selectedSquare = null) }
        if (move != null) {
            makeMoveOnBoard(move)
        }
    }

    fun cancelPromotion() {
        _uiState.update { it.copy(pendingPromotionMove = null) }
    }

    private fun makeMoveOnBoard(move: ChessMove) {
        val state = _uiState.value
        val beforeBoard = state.boardState
        val san = ChessEngine.moveToSan(beforeBoard, move)
        val nextBoard = ChessEngine.applyMove(beforeBoard, move)
        val nextFen = nextBoard.toFen()
        val nextLegal = ChessEngine.generateLegalMoves(nextBoard)

        // Active opening repertoire trainer check
        val practice = state.activePracticeEntry
        if (practice != null) {
            val expectedTokens = practice.pgnMoves
                .replace(Regex("\\d+\\.+"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            val expectedSan = expectedTokens.getOrNull(state.practiceStepIndex)
            if (expectedSan != null) {
                val cleanExpected = expectedSan.replace(Regex("[+#?!]+$"), "")
                val cleanPlayed = san.replace(Regex("[+#?!]+$"), "")
                if (cleanExpected != cleanPlayed) {
                    _uiState.update {
                        it.copy(
                            selectedSquare = null,
                            practiceFeedback = "Неверно: вы сыграли $san, а по теории требуется $expectedSan"
                        )
                    }
                    return
                }
            }
        }

        val truncatedNodes = state.moveNodes.take(state.currentPly).toMutableList()
        val newPly = truncatedNodes.size + 1
        val moveNum = ((newPly - 1) / 2) + 1
        val newNode = MoveTreeNode(
            id = "ply_${newPly}_${move.uci}",
            parentId = truncatedNodes.lastOrNull()?.id,
            ply = newPly,
            moveNumber = moveNum,
            color = beforeBoard.sideToMove,
            san = san,
            uci = move.uci,
            from = move.fromAlgebraic,
            to = move.toAlgebraic,
            fenBefore = beforeBoard.toFen(),
            fenAfter = nextFen
        )
        truncatedNodes.add(newNode)

        val openingSummary = computeOpeningSummary(truncatedNodes, state.headers)

        _uiState.update {
            it.copy(
                currentFen = nextFen,
                boardState = nextBoard,
                legalMoves = nextLegal,
                moveNodes = truncatedNodes,
                currentPly = newPly,
                selectedSquare = null,
                pendingPromotionMove = null,
                openingSummary = openingSummary,
                headers = it.headers.copy(
                    eco = openingSummary.eco,
                    opening = if (openingSummary.variation != null) {
                        "${openingSummary.name}: ${openingSummary.variation}"
                    } else {
                        openingSummary.name
                    }
                )
            )
        }

        evaluateCurrentPositionAsync(nextFen)

        // Advance repertoire trainer if active
        if (practice != null) {
            advancePracticeIfNeeded(practice, newPly, nextBoard)
        }
    }

    private fun advancePracticeIfNeeded(
        practice: RepertoireEntry,
        currentPly: Int,
        boardAfterUserMove: ChessBoardState
    ) {
        val tokens = practice.pgnMoves
            .replace(Regex("\\d+\\.+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (currentPly >= tokens.size) {
            val updated = practice.copy(
                practicedCount = practice.practicedCount + 1,
                correctCount = practice.correctCount + 1,
                lastPracticedAt = System.currentTimeMillis()
            )
            db.saveRepertoireEntry(updated)
            _uiState.update {
                it.copy(
                    repertoire = db.getAllRepertoire(),
                    practiceStepIndex = currentPly,
                    practiceFeedback = "Отлично! Линия «${practice.title}» успешно пройдена до конца!"
                )
            }
            return
        }

        val replySan = tokens[currentPly]
        val replyMove = ChessEngine.parseSanMove(boardAfterUserMove, replySan)
        if (replyMove != null) {
            val canonicalReplySan = ChessEngine.moveToSan(boardAfterUserMove, replyMove)
            val boardAfterReply = ChessEngine.applyMove(boardAfterUserMove, replyMove)
            val fenAfterReply = boardAfterReply.toFen()
            val legalAfterReply = ChessEngine.generateLegalMoves(boardAfterReply)
            val replyPly = currentPly + 1
            val replyNode = MoveTreeNode(
                id = "ply_${replyPly}_${replyMove.uci}",
                parentId = _uiState.value.moveNodes.lastOrNull()?.id,
                ply = replyPly,
                moveNumber = ((replyPly - 1) / 2) + 1,
                color = boardAfterUserMove.sideToMove,
                san = canonicalReplySan,
                uci = replyMove.uci,
                from = replyMove.fromAlgebraic,
                to = replyMove.toAlgebraic,
                fenBefore = boardAfterUserMove.toFen(),
                fenAfter = fenAfterReply
            )
            val updatedNodes = _uiState.value.moveNodes + replyNode
            val done = replyPly >= tokens.size
            if (done) {
                val updated = practice.copy(
                    practicedCount = practice.practicedCount + 1,
                    correctCount = practice.correctCount + 1,
                    lastPracticedAt = System.currentTimeMillis()
                )
                db.saveRepertoireEntry(updated)
            }
            _uiState.update {
                it.copy(
                    currentFen = fenAfterReply,
                    boardState = boardAfterReply,
                    legalMoves = legalAfterReply,
                    moveNodes = updatedNodes,
                    currentPly = replyPly,
                    practiceStepIndex = replyPly,
                    repertoire = if (done) db.getAllRepertoire() else it.repertoire,
                    practiceFeedback = if (done) {
                        "Отлично! Линия «${practice.title}» полностью пройдена!"
                    } else {
                        "Соперник ответил $canonicalReplySan. Ваш следующий ход!"
                    }
                )
            }
        }
    }

    fun jumpToPly(targetPly: Int) {
        val state = _uiState.value
        val clamped = targetPly.coerceIn(0, state.moveNodes.size)
        val targetFen = if (clamped == 0) {
            state.initialFen
        } else {
            state.moveNodes[clamped - 1].fenAfter
        }
        val board = ChessEngine.parseFen(targetFen)
        val legal = ChessEngine.generateLegalMoves(board)

        val analyzedMove = state.analyses.getOrNull(clamped - 1)
        val preEval = analyzedMove?.evalAfter
        val preLines = analyzedMove?.topLines ?: emptyList()

        _uiState.update {
            it.copy(
                currentPly = clamped,
                currentFen = targetFen,
                boardState = board,
                legalMoves = legal,
                selectedSquare = null,
                pendingPromotionMove = null,
                userArrows = emptyList(),
                squareHighlights = emptyList(),
                currentEvaluation = preEval ?: it.currentEvaluation,
                currentTopLines = if (preLines.isNotEmpty()) preLines else it.currentTopLines
            )
        }

        if (preEval == null || preLines.isEmpty()) {
            evaluateCurrentPositionAsync(targetFen)
        }
    }

    fun stepForward() = jumpToPly(_uiState.value.currentPly + 1)
    fun stepBackward() = jumpToPly(_uiState.value.currentPly - 1)
    fun jumpToStart() = jumpToPly(0)
    fun jumpToEnd() = jumpToPly(_uiState.value.moveNodes.size)

    fun flipBoard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun toggleAnnotationMode() {
        _uiState.update {
            it.copy(
                annotationMode = !it.annotationMode,
                annotationArrowStartSq = null,
                selectedSquare = null
            )
        }
    }

    fun toggleUserArrow(from: String, to: String) {
        if (from == to) return
        val state = _uiState.value
        val color = state.settings.userArrowColorHex
        val exists = state.userArrows.any { it.from == from && it.to == to }
        val next = if (exists) {
            state.userArrows.filterNot { it.from == from && it.to == to }
        } else {
            state.userArrows + UserArrow(from = from, to = to, colorHex = color)
        }
        _uiState.update { it.copy(userArrows = next) }
    }

    fun toggleSquareHighlight(square: String) {
        val state = _uiState.value
        val color = "rgba(239, 68, 68, 0.45)"
        val exists = state.squareHighlights.any { it.square == square }
        val next = if (exists) {
            state.squareHighlights.filterNot { it.square == square }
        } else {
            state.squareHighlights + SquareHighlight(square = square, colorHex = color)
        }
        _uiState.update { it.copy(squareHighlights = next) }
    }

    fun clearAnnotations() {
        _uiState.update {
            it.copy(
                userArrows = emptyList(),
                squareHighlights = emptyList(),
                annotationArrowStartSq = null
            )
        }
    }

    private fun evaluateCurrentPositionAsync(fen: String) {
        liveEvalJob?.cancel()
        liveEvalJob = viewModelScope.launch(Dispatchers.Default) {
            val cfg = _uiState.value.settings.engineConfig
            val bridgeResult = stockfishBridge.evaluatePosition(
                fen = fen,
                config = cfg.copy(depth = minOf(cfg.depth, 16))
            ) { liveEv, liveLines ->
                _uiState.update {
                    if (it.currentFen == fen) {
                        it.copy(currentEvaluation = liveEv, currentTopLines = liveLines)
                    } else it
                }
            }
            val finalRes = bridgeResult ?: ChessAnalyzerEngine.evaluatePositionFallback(fen, cfg)
            _uiState.update {
                if (it.currentFen == fen) {
                    it.copy(
                        currentEvaluation = finalRes.evaluation,
                        currentTopLines = finalRes.lines
                    )
                } else it
            }
        }
    }

    fun runFullGameAnalysis() {
        val nodes = _uiState.value.moveNodes
        if (nodes.isEmpty()) return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    analysisProgress = 0f,
                    analysisPlyText = "Подготовка Stockfish UCI..."
                )
            }

            val cfg = _uiState.value.settings.engineConfig
            val thresholds = _uiState.value.settings.thresholds

            val (analyses, stats) = ChessAnalyzerEngine.analyzeFullGame(
                nodes = nodes,
                config = cfg,
                thresholds = thresholds,
                stockfishBridge = stockfishBridge,
                onProgress = { ply, total, moveAnalysis ->
                    _uiState.update { state ->
                        val updatedNodes = state.moveNodes.map { n ->
                            if (n.ply == ply) n.copy(analysis = moveAnalysis) else n
                        }
                        state.copy(
                            moveNodes = updatedNodes,
                            analysisProgress = ply.toFloat() / total.toFloat(),
                            analysisPlyText = "Ход $ply из $total (${moveAnalysis.san})"
                        )
                    }
                }
            )

            val currentPly = _uiState.value.currentPly
            val currentMoveEval = if (currentPly > 0) analyses.getOrNull(currentPly - 1)?.evalAfter else null
            val currentMoveLines = if (currentPly > 0) analyses.getOrNull(currentPly - 1)?.topLines else null

            _uiState.update { state ->
                val syncedNodes = state.moveNodes.mapIndexed { idx, node ->
                    node.copy(analysis = analyses.getOrNull(idx))
                }
                state.copy(
                    isAnalyzing = false,
                    analysisProgress = 1f,
                    analysisPlyText = "Анализ завершён",
                    moveNodes = syncedNodes,
                    analyses = analyses,
                    gameStats = stats,
                    currentEvaluation = currentMoveEval ?: state.currentEvaluation,
                    currentTopLines = if (!currentMoveLines.isNullOrEmpty()) currentMoveLines else state.currentTopLines
                )
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        stockfishBridge.stop()
        _uiState.update { it.copy(isAnalyzing = false, analysisPlyText = "Анализ остановлен") }
    }

    fun saveCurrentGameToSqlite(titleOverride: String? = null, folderTag: String = "Мои партии") {
        val state = _uiState.value
        val pgnMoves = buildString {
            for (node in state.moveNodes) {
                if (node.color == PieceColor.WHITE) {
                    append("${node.moveNumber}. ${node.san} ")
                } else {
                    append("${node.san} ")
                }
            }
            append(state.headers.result)
        }.trim()

        val fullPgn = """
            [Event "${state.headers.event}"]
            [Site "${state.headers.site}"]
            [Date "${state.headers.date}"]
            [White "${state.headers.white}"]
            [Black "${state.headers.black}"]
            [Result "${state.headers.result}"]
            [WhiteElo "${state.headers.whiteElo}"]
            [BlackElo "${state.headers.blackElo}"]
            [ECO "${state.headers.eco}"]
            [Opening "${state.headers.opening}"]
            [FEN "${state.initialFen}"]

            $pgnMoves
        """.trimIndent()

        val record = SavedGameRecord(
            id = UUID.randomUUID().toString(),
            title = titleOverride ?: "${state.headers.white} vs ${state.headers.black}",
            white = state.headers.white,
            black = state.headers.black,
            whiteElo = state.headers.whiteElo,
            blackElo = state.headers.blackElo,
            result = state.headers.result,
            event = state.headers.event,
            date = state.headers.date,
            eco = state.headers.eco,
            openingName = state.headers.opening,
            pgn = fullPgn,
            initialFen = state.initialFen,
            movesCount = state.moveNodes.size,
            whiteAccuracy = state.gameStats.white.accuracy.takeIf { state.analyses.isNotEmpty() },
            blackAccuracy = state.gameStats.black.accuracy.takeIf { state.analyses.isNotEmpty() },
            isFavorite = false,
            folderTag = folderTag,
            createdAt = System.currentTimeMillis(),
            analysesJson = if (state.analyses.isNotEmpty()) db.serializeAnalyses(state.analyses) else null
        )

        db.saveGame(record)
        _uiState.update { it.copy(savedGames = db.getAllSavedGames()) }
    }

    fun openSavedGame(record: SavedGameRecord) {
        val restoredAnalyses = db.deserializeAnalyses(record.analysesJson)
        loadPgnGame(
            pgnText = record.pgn,
            autoAnalyze = restoredAnalyses.isEmpty(),
            preloadedAnalyses = restoredAnalyses
        )
    }

    fun toggleFavoriteGame(record: SavedGameRecord) {
        db.toggleFavoriteGame(record.id, !record.isFavorite)
        _uiState.update { it.copy(savedGames = db.getAllSavedGames()) }
    }

    fun deleteSavedGame(id: String) {
        db.deleteGame(id)
        _uiState.update { it.copy(savedGames = db.getAllSavedGames()) }
    }

    // =========================================================================
    // Full Chess.com PubAPI Multi-Archive Browser (matching Web ChessComModal.tsx)
    // =========================================================================

    fun searchChessComPlayer(usernameInput: String) {
        val clean = usernameInput.trim()
        if (clean.isEmpty()) {
            _uiState.update { it.copy(chessComError = "Введите имя пользователя Chess.com") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    chessComUsername = clean,
                    chessComLoading = true,
                    chessComError = null,
                    chessComProfile = null,
                    chessComArchiveUrls = emptyList(),
                    chessComLoadedArchiveCount = 0,
                    chessComGames = emptyList(),
                    chessComSelectedArchiveUrl = "all_loaded"
                )
            }

            val profileRes = OnlineImportService.fetchChessComProfile(clean)
            val archivesRes = OnlineImportService.fetchChessComArchives(clean)

            if (profileRes.isFailure) {
                _uiState.update {
                    it.copy(
                        chessComLoading = false,
                        chessComError = profileRes.exceptionOrNull()?.message
                            ?: "Не удалось загрузить профиль игрока Chess.com"
                    )
                }
                return@launch
            }

            val profile = profileRes.getOrThrow()
            val archives = archivesRes.getOrDefault(emptyList())

            if (archives.isEmpty()) {
                _uiState.update {
                    it.copy(
                        chessComProfile = profile,
                        chessComLoading = false,
                        chessComError = "У данного игрока нет доступных архивов партий на Chess.com."
                    )
                }
                return@launch
            }

            val initialBatch = archives.take(INITIAL_ARCHIVE_BATCH_SIZE)
            var mergedGames = emptyList<ChessComGameItem>()
            for (url in initialBatch) {
                val monthGames = OnlineImportService.fetchChessComGamesByArchiveUrl(url).getOrDefault(emptyList())
                mergedGames = OnlineImportService.mergeAndDeduplicateChessComGames(mergedGames, monthGames)
            }

            _uiState.update {
                it.copy(
                    chessComProfile = profile,
                    chessComArchiveUrls = archives,
                    chessComLoadedArchiveCount = initialBatch.size,
                    chessComGames = mergedGames,
                    chessComSelectedArchiveUrl = "all_loaded",
                    chessComLoading = false,
                    chessComError = if (mergedGames.isEmpty()) "В последних архивах нет стандартных шахматных партий с PGN." else null
                )
            }
        }
    }

    fun loadMoreChessComArchives() {
        val state = _uiState.value
        if (state.chessComLoadingMore || state.chessComLoadedArchiveCount >= state.chessComArchiveUrls.size) return

        viewModelScope.launch {
            _uiState.update { it.copy(chessComLoadingMore = true, chessComError = null) }
            val nextBatch = state.chessComArchiveUrls.slice(
                state.chessComLoadedArchiveCount until minOf(
                    state.chessComArchiveUrls.size,
                    state.chessComLoadedArchiveCount + LOAD_MORE_ARCHIVE_BATCH_SIZE
                )
            )
            var merged = state.chessComGames
            for (url in nextBatch) {
                val monthGames = OnlineImportService.fetchChessComGamesByArchiveUrl(url).getOrDefault(emptyList())
                merged = OnlineImportService.mergeAndDeduplicateChessComGames(merged, monthGames)
            }
            _uiState.update {
                it.copy(
                    chessComGames = merged,
                    chessComLoadedArchiveCount = state.chessComLoadedArchiveCount + nextBatch.size,
                    chessComSelectedArchiveUrl = "all_loaded",
                    chessComLoadingMore = false
                )
            }
        }
    }

    fun loadAllRemainingChessComArchives() {
        val state = _uiState.value
        if (state.chessComLoadingMore || state.chessComLoadedArchiveCount >= state.chessComArchiveUrls.size) return

        viewModelScope.launch {
            _uiState.update { it.copy(chessComLoadingMore = true, chessComError = null) }
            val remaining = state.chessComArchiveUrls.drop(state.chessComLoadedArchiveCount)
            var merged = state.chessComGames
            for (url in remaining) {
                val monthGames = OnlineImportService.fetchChessComGamesByArchiveUrl(url).getOrDefault(emptyList())
                merged = OnlineImportService.mergeAndDeduplicateChessComGames(merged, monthGames)
            }
            _uiState.update {
                it.copy(
                    chessComGames = merged,
                    chessComLoadedArchiveCount = state.chessComArchiveUrls.size,
                    chessComSelectedArchiveUrl = "all_loaded",
                    chessComLoadingMore = false
                )
            }
        }
    }

    fun selectChessComArchiveUrl(url: String) {
        if (url == "all_loaded") {
            _uiState.update { it.copy(chessComSelectedArchiveUrl = "all_loaded") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    chessComSelectedArchiveUrl = url,
                    chessComLoading = true,
                    chessComError = null
                )
            }
            val res = OnlineImportService.fetchChessComGamesByArchiveUrl(url)
            res.fold(
                onSuccess = { monthGames ->
                    _uiState.update {
                        it.copy(
                            chessComGames = monthGames,
                            chessComLoading = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            chessComLoading = false,
                            chessComError = err.message ?: "Ошибка загрузки выбранного месяца"
                        )
                    }
                }
            )
        }
    }

    fun updateChessComFilters(
        resultFilter: String = _uiState.value.chessComResultFilter,
        colorFilter: String = _uiState.value.chessComColorFilter,
        timeClassFilter: String = _uiState.value.chessComTimeClassFilter
    ) {
        _uiState.update {
            it.copy(
                chessComResultFilter = resultFilter,
                chessComColorFilter = colorFilter,
                chessComTimeClassFilter = timeClassFilter
            )
        }
    }

    fun importSingleChessComGame(game: ChessComGameItem) {
        val parsed = ChessEngine.parsePgn(game.pgn)
        val record = SavedGameRecord(
            id = game.uuid ?: UUID.randomUUID().toString(),
            title = "${game.white.username} (${game.white.rating}) vs ${game.black.username} (${game.black.rating})",
            white = game.white.username,
            black = game.black.username,
            whiteElo = game.white.rating.toString(),
            blackElo = game.black.rating.toString(),
            result = parsed.headers.result,
            event = "Chess.com ${game.timeClass.uppercase()}",
            date = parsed.headers.date,
            eco = parsed.headers.eco,
            openingName = parsed.headers.opening,
            pgn = game.pgn,
            initialFen = parsed.initialFen,
            movesCount = parsed.nodes.size,
            whiteAccuracy = null,
            blackAccuracy = null,
            isFavorite = false,
            folderTag = "Chess.com (${game.timeClass})",
            createdAt = if (game.endTime > 0) game.endTime * 1000L else System.currentTimeMillis()
        )
        db.saveGame(record)
        _uiState.update { it.copy(savedGames = db.getAllSavedGames()) }
        loadPgnGame(game.pgn, autoAnalyze = true)
    }

    fun importFromOnlinePlatform(platform: String, username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImportingOnline = true,
                    onlineImportMessage = "Загрузка партий пользователя $username с $platform..."
                )
            }

            val result = if (platform.equals("lichess", ignoreCase = true)) {
                OnlineImportService.fetchLichessGames(username, maxGames = 15)
            } else {
                OnlineImportService.fetchChessComGames(username, maxGames = 20)
            }

            result.fold(
                onSuccess = { pgns ->
                    var importedCount = 0
                    for (pgn in pgns) {
                        val parsed = ChessEngine.parsePgn(pgn)
                        if (parsed.nodes.isNotEmpty()) {
                            val rec = SavedGameRecord(
                                id = UUID.randomUUID().toString(),
                                title = "${parsed.headers.white} vs ${parsed.headers.black}",
                                white = parsed.headers.white,
                                black = parsed.headers.black,
                                whiteElo = parsed.headers.whiteElo,
                                blackElo = parsed.headers.blackElo,
                                result = parsed.headers.result,
                                event = parsed.headers.event,
                                date = parsed.headers.date,
                                eco = parsed.headers.eco,
                                openingName = parsed.headers.opening,
                                pgn = pgn,
                                initialFen = parsed.initialFen,
                                movesCount = parsed.nodes.size,
                                whiteAccuracy = null,
                                blackAccuracy = null,
                                isFavorite = false,
                                folderTag = "$platform ($username)",
                                createdAt = System.currentTimeMillis() - importedCount * 1000L
                            )
                            db.saveGame(rec)
                            importedCount++
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isImportingOnline = false,
                            savedGames = db.getAllSavedGames(),
                            onlineImportMessage = "Успешно импортировано партий: $importedCount"
                        )
                    }
                    if (pgns.isNotEmpty()) {
                        loadPgnGame(pgns.first(), autoAnalyze = true)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isImportingOnline = false,
                            onlineImportMessage = "Ошибка: ${err.message ?: "Не удалось загрузить партии"}"
                        )
                    }
                }
            )
        }
    }

    // =========================================================================
    // Opening Repertoire Trainer
    // =========================================================================

    fun saveRepertoireLine(
        title: String,
        color: PieceColor,
        eco: String,
        pgnMoves: String,
        notes: String,
        tags: String
    ) {
        val parsed = ChessEngine.parsePgn(pgnMoves)
        val targetFen = parsed.nodes.lastOrNull()?.fenAfter ?: ChessEngine.INITIAL_FEN
        val entry = RepertoireEntry(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { parsed.headers.opening },
            color = color,
            eco = eco.ifBlank { parsed.headers.eco },
            pgnMoves = pgnMoves,
            targetFen = targetFen,
            notes = notes,
            tags = tags.ifBlank { "Пользовательский" },
            practicedCount = 0,
            correctCount = 0,
            lastPracticedAt = null
        )
        db.saveRepertoireEntry(entry)
        _uiState.update { it.copy(repertoire = db.getAllRepertoire()) }
    }

    fun deleteRepertoireLine(id: String) {
        db.deleteRepertoireEntry(id)
        _uiState.update { it.copy(repertoire = db.getAllRepertoire()) }
    }

    fun startRepertoirePractice(entry: RepertoireEntry) {
        analysisJob?.cancel()
        val startBoard = ChessEngine.parseFen(ChessEngine.INITIAL_FEN)
        val legal = ChessEngine.generateLegalMoves(startBoard)
        _uiState.update {
            it.copy(
                headers = GameHeaders(
                    event = "Тренировка репертуара: ${entry.title}",
                    white = if (entry.color == PieceColor.WHITE) "Вы" else "Тренер",
                    black = if (entry.color == PieceColor.BLACK) "Вы" else "Тренер",
                    eco = entry.eco,
                    opening = entry.title
                ),
                initialFen = ChessEngine.INITIAL_FEN,
                currentFen = ChessEngine.INITIAL_FEN,
                boardState = startBoard,
                legalMoves = legal,
                moveNodes = emptyList(),
                currentPly = 0,
                selectedSquare = null,
                isFlipped = entry.color == PieceColor.BLACK,
                activePracticeEntry = entry,
                practiceStepIndex = 0,
                practiceFeedback = "Сделайте первый ход по линии «${entry.title}» (${entry.eco})"
            )
        }

        if (entry.color == PieceColor.BLACK) {
            advancePracticeIfNeeded(entry, 0, startBoard)
        }
    }

    fun stopRepertoirePractice() {
        _uiState.update {
            it.copy(
                activePracticeEntry = null,
                practiceStepIndex = 0,
                practiceFeedback = null
            )
        }
    }

    // =========================================================================
    // Synchronized Settings & Immediate Threshold Reclassification
    // =========================================================================

    fun updateSettings(newSettings: AppSettings) {
        val prevSettings = _uiState.value.settings
        db.saveSettings(newSettings)

        val thresholdsChanged = prevSettings.thresholds != newSettings.thresholds
        val engineConfigChanged = prevSettings.engineConfig != newSettings.engineConfig

        val currentAnalyses = _uiState.value.analyses
        if (thresholdsChanged && currentAnalyses.isNotEmpty()) {
            val reclassified = ChessAnalyzerEngine.reclassifyMoveAnalyses(
                analyses = currentAnalyses,
                thresholds = newSettings.thresholds
            )
            val newStats = AccuracyCalculator.calculateGameStatistics(reclassified)
            val updatedNodes = _uiState.value.moveNodes.mapIndexed { idx, node ->
                node.copy(analysis = reclassified.getOrNull(idx))
            }
            _uiState.update {
                it.copy(
                    settings = newSettings,
                    analyses = reclassified,
                    gameStats = newStats,
                    moveNodes = updatedNodes
                )
            }
        } else {
            _uiState.update { it.copy(settings = newSettings) }
        }

        if (engineConfigChanged) {
            viewModelScope.launch(Dispatchers.Default) {
                stockfishBridge.applyConfig(newSettings.engineConfig)
            }
        }
    }

    fun applyBoardThemePreset(presetId: String) {
        val preset = BOARD_THEME_PRESETS.firstOrNull { it.id == presetId } ?: return
        val next = _uiState.value.settings.copy(
            boardThemeId = preset.id,
            lightSquareColorHex = preset.lightSquareHex,
            darkSquareColorHex = preset.darkSquareHex
        )
        updateSettings(next)
    }

    fun updateThresholds(newThresholds: ClassificationThresholds) {
        updateSettings(_uiState.value.settings.copy(thresholds = newThresholds))
    }

    fun updateEngineConfig(newEngineConfig: EngineConfig) {
        updateSettings(_uiState.value.settings.copy(engineConfig = newEngineConfig))
    }

    fun resetThresholdsToDefaults() {
        updateThresholds(ChessAnalyzerEngine.DEFAULT_THRESHOLDS)
    }

    fun resetSettings() {
        updateSettings(AppSettings())
    }
}
