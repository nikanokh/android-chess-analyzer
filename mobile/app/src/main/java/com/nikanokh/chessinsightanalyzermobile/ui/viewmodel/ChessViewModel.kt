package com.nikanokh.chessinsightanalyzermobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikanokh.chessinsightanalyzermobile.data.local.ChessSqliteDatabase
import com.nikanokh.chessinsightanalyzermobile.data.remote.OnlineImportService
import com.nikanokh.chessinsightanalyzermobile.domain.chess.*
import com.nikanokh.chessinsightanalyzermobile.domain.engine.ChessAnalyzerEngine
import com.nikanokh.chessinsightanalyzermobile.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

enum class AppTab(val titleRu: String) {
    HOME("Главная"),
    BOARD("Доска"),
    GAMES("Партии"),
    REPERTOIRE("Дебюты"),
    SETTINGS("Настройки")
}

data class ChessUiState(
    val activeTab: AppTab = AppTab.HOME,
    val activeGameId: String = "",
    val metadata: GameMetadata = GameMetadata(),
    val opening: OpeningInfo = OpeningInfo("C41", "Защита Филидора", "Вариант 3...Bg4 (Опера в Париже)"),
    val moves: List<MoveNode> = emptyList(),
    val currentPly: Int = 0, // 0 = initial position, 1..moves.size
    val selectedSquare: Square? = null,
    val legalDestinations: Set<Square> = emptySet(),
    val statistics: GameStatistics? = null,
    val isAnalyzing: Boolean = false,
    val analysisCurrent: Int = 0,
    val analysisTotal: Int = 0,
    val savedGames: List<SavedGameRecord> = emptyList(),
    val repertoireOpenings: List<RepertoireOpeningStat> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val statusMessage: String? = null,
    val isImportingOnline: Boolean = false,
    val lastBoardMoveScrollTick: Int = 0
) {
    val currentNode: MoveNode?
        get() = if (currentPly in 1..moves.size) moves[currentPly - 1] else null

    val currentFen: String
        get() = currentNode?.fenAfter ?: INITIAL_FEN

    val currentPosition: ChessPosition
        get() = ChessPosition.fromFen(currentFen)

    val capturedSummary: CapturedSummary
        get() = PgnParser.calculateCapturedSummary(currentFen)
}

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val sqliteDb = ChessSqliteDatabase(application.applicationContext)
    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            sqliteDb.ensureDefaultMasterGamesSeeded()
            val allGames = sqliteDb.loadAllGames()
            val loadedSettings = sqliteDb.loadSettings()
            val repertoire = buildRepertoireFromGames(allGames)
            val firstGame = allGames.firstOrNull()

            withContext(Dispatchers.Main) {
                if (firstGame != null) {
                    _uiState.update {
                        it.copy(
                            activeGameId = firstGame.id,
                            metadata = firstGame.metadata,
                            opening = firstGame.opening,
                            moves = firstGame.moves,
                            currentPly = firstGame.moves.size,
                            statistics = firstGame.statistics,
                            savedGames = allGames,
                            repertoireOpenings = repertoire,
                            settings = loadedSettings
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            savedGames = allGames,
                            repertoireOpenings = repertoire,
                            settings = loadedSettings
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab, statusMessage = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun goToStart() {
        _uiState.update { it.copy(currentPly = 0, selectedSquare = null, legalDestinations = emptySet()) }
    }

    fun goToPrev() {
        _uiState.update {
            it.copy(
                currentPly = (it.currentPly - 1).coerceAtLeast(0),
                selectedSquare = null,
                legalDestinations = emptySet()
            )
        }
    }

    fun goToNext() {
        _uiState.update {
            it.copy(
                currentPly = (it.currentPly + 1).coerceAtMost(it.moves.size),
                selectedSquare = null,
                legalDestinations = emptySet()
            )
        }
    }

    fun goToEnd() {
        _uiState.update {
            it.copy(
                currentPly = it.moves.size,
                selectedSquare = null,
                legalDestinations = emptySet()
            )
        }
    }

    fun selectPly(ply: Int) {
        _uiState.update {
            it.copy(
                currentPly = ply.coerceIn(0, it.moves.size),
                selectedSquare = null,
                legalDestinations = emptySet()
            )
        }
    }

    fun onSquareClicked(square: Square) {
        val state = _uiState.value
        val pos = state.currentPosition
        val currentSelected = state.selectedSquare

        // Check if user tapped a legal destination square to execute a move
        if (currentSelected != null && currentSelected != square && square in state.legalDestinations) {
            val legalMoves = pos.generateLegalMoves()
            val chosenMove = legalMoves.firstOrNull {
                it.from == currentSelected &&
                    it.to == square &&
                    (it.promotion == null || it.promotion == PieceType.QUEEN)
            }
            if (chosenMove != null) {
                executeMoveOnBoard(pos, chosenMove, legalMoves)
                return
            }
        }

        // Otherwise select friendly piece
        val tappedPiece = pos.pieceAt(square)
        if (tappedPiece != null && tappedPiece.color == pos.sideToMove) {
            if (currentSelected == square) {
                _uiState.update { it.copy(selectedSquare = null, legalDestinations = emptySet()) }
            } else {
                val dests = pos.generateLegalMoves()
                    .filter { it.from == square }
                    .map { it.to }
                    .toSet()
                _uiState.update { it.copy(selectedSquare = square, legalDestinations = dests) }
            }
        } else {
            _uiState.update { it.copy(selectedSquare = null, legalDestinations = emptySet()) }
        }
    }

    private fun executeMoveOnBoard(
        pos: ChessPosition,
        move: ChessMove,
        legalMoves: List<ChessMove>
    ) {
        val state = _uiState.value
        val prefixMoves = state.moves.take(state.currentPly)
        val newPly = prefixMoves.size + 1
        val san = pos.toSan(move, legalMoves)
        val fenBefore = pos.toFen()
        val nextPos = pos.applyMove(move)
        val fenAfter = nextPos.toFen()

        // Instant lightweight eval for the played move
        val beforeEval = ChessAnalyzerEngine.evaluateStaticWhitePov(pos)
        val afterEval = ChessAnalyzerEngine.evaluateStaticWhitePov(nextPos)
        val (cpl, cat) = ChessAnalyzerEngine.classifyMove(
            ply = newPly,
            moverColor = pos.sideToMove,
            playedUci = move.uci,
            bestMoveUci = move.uci,
            evalBeforeWhiteCp = beforeEval,
            evalAfterWhiteCp = afterEval
        )

        val newNode = MoveNode(
            id = "ply_$newPly",
            ply = newPly,
            moveNumber = pos.fullmoveNumber,
            color = pos.sideToMove,
            san = san,
            uci = move.uci,
            fromSquare = move.from.algebraic,
            toSquare = move.to.algebraic,
            fenBefore = fenBefore,
            fenAfter = fenAfter,
            analysis = MoveAnalysis(
                evalBeforeCp = beforeEval,
                evalAfterCp = afterEval,
                cpl = cpl,
                bestMoveUci = move.uci,
                bestMoveSan = san,
                category = cat
            )
        )

        val updatedMoves = prefixMoves + newNode
        val detectedOpening = EcoOpeningBook.detectOpening(updatedMoves.map { it.san }, state.metadata.eco)
        val updatedMeta = state.metadata.copy(eco = detectedOpening.eco)

        _uiState.update {
            it.copy(
                moves = updatedMoves,
                currentPly = updatedMoves.size,
                opening = detectedOpening,
                metadata = updatedMeta,
                selectedSquare = null,
                legalDestinations = emptySet(),
                lastBoardMoveScrollTick = it.lastBoardMoveScrollTick + 1
            )
        }
    }

    fun startNewGame() {
        analysisJob?.cancel()
        val newMeta = GameMetadata(
            whiteName = "Игрок (Белые)",
            blackName = "Игрок (Чёрные)",
            result = "*",
            date = "2025.02.25",
            event = "Свободная партия",
            site = "Android Local",
            eco = "A00",
            platform = "Local"
        )
        val opening = OpeningInfo("A00", "Начальная позиция", "Стандартная расстановка")
        _uiState.update {
            it.copy(
                activeGameId = UUID.randomUUID().toString(),
                metadata = newMeta,
                opening = opening,
                moves = emptyList(),
                currentPly = 0,
                selectedSquare = null,
                legalDestinations = emptySet(),
                statistics = null,
                activeTab = AppTab.BOARD
            )
        }
    }

    fun loadSavedGame(record: SavedGameRecord) {
        analysisJob?.cancel()
        _uiState.update {
            it.copy(
                activeGameId = record.id,
                metadata = record.metadata,
                opening = record.opening,
                moves = record.moves,
                currentPly = record.moves.size,
                statistics = record.statistics,
                selectedSquare = null,
                legalDestinations = emptySet(),
                activeTab = AppTab.BOARD
            )
        }
    }

    fun loadPgnString(rawPgn: String, platform: String = "PGN", switchToBoard: Boolean = true): Boolean {
        val chunks = OnlineImportService.splitMultiPgn(rawPgn)
        if (chunks.isEmpty()) return false

        var firstRecord: SavedGameRecord? = null
        for (chunk in chunks) {
            val parsed = PgnParser.parsePgn(chunk, platform)
            if (parsed.moves.isEmpty()) continue
            val record = SavedGameRecord(
                id = UUID.randomUUID().toString(),
                dedupKey = sqliteDb.computeDedupKey(parsed.metadata, parsed.moves),
                createdAt = System.currentTimeMillis(),
                metadata = parsed.metadata,
                opening = parsed.opening,
                pgn = parsed.rawPgn,
                finalFen = parsed.finalFen,
                statistics = null,
                moves = parsed.moves
            )
            sqliteDb.upsertGame(record)
            if (firstRecord == null) firstRecord = record
        }

        val chosen = firstRecord ?: return false
        val allGames = sqliteDb.loadAllGames()
        val repertoire = buildRepertoireFromGames(allGames)

        _uiState.update {
            it.copy(
                activeGameId = chosen.id,
                metadata = chosen.metadata,
                opening = chosen.opening,
                moves = chosen.moves,
                currentPly = chosen.moves.size,
                statistics = chosen.statistics,
                savedGames = allGames,
                repertoireOpenings = repertoire,
                selectedSquare = null,
                legalDestinations = emptySet(),
                activeTab = if (switchToBoard) AppTab.BOARD else it.activeTab,
                statusMessage = "Импортировано партий в SQLite: ${chunks.size}"
            )
        }
        return true
    }

    fun importOnlineGames(platform: String, username: String) {
        if (username.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Введите никнейм игрока") }
            return
        }
        _uiState.update { it.copy(isImportingOnline = true, statusMessage = "Загрузка партий с $platform...") }
        viewModelScope.launch {
            val result = if (platform.equals("Lichess", ignoreCase = true)) {
                OnlineImportService.fetchLichessGamesPgn(username, 12)
            } else {
                OnlineImportService.fetchChessComGamesPgn(username, 12)
            }
            result.onSuccess { pgnList ->
                if (pgnList.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isImportingOnline = false,
                            statusMessage = "Партии пользователя $username не найдены"
                        )
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        for (pgn in pgnList) {
                            val parsed = PgnParser.parsePgn(pgn, platform)
                            if (parsed.moves.isEmpty()) continue
                            val record = SavedGameRecord(
                                id = UUID.randomUUID().toString(),
                                dedupKey = sqliteDb.computeDedupKey(parsed.metadata, parsed.moves),
                                createdAt = System.currentTimeMillis(),
                                metadata = parsed.metadata,
                                opening = parsed.opening,
                                pgn = parsed.rawPgn,
                                finalFen = parsed.finalFen,
                                statistics = null,
                                moves = parsed.moves
                            )
                            sqliteDb.upsertGame(record)
                        }
                    }
                    val allGames = withContext(Dispatchers.IO) { sqliteDb.loadAllGames() }
                    val repertoire = buildRepertoireFromGames(allGames)
                    _uiState.update {
                        it.copy(
                            isImportingOnline = false,
                            savedGames = allGames,
                            repertoireOpenings = repertoire,
                            statusMessage = "Загружено ${pgnList.size} партий ($platform) в SQLite"
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isImportingOnline = false,
                        statusMessage = "Ошибка загрузки: ${err.localizedMessage ?: "Проверьте интернет"}"
                    )
                }
            }
        }
    }

    fun deleteSavedGame(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sqliteDb.deleteGame(id)
            val allGames = sqliteDb.loadAllGames()
            val repertoire = buildRepertoireFromGames(allGames)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        savedGames = allGames,
                        repertoireOpenings = repertoire
                    )
                }
            }
        }
    }

    fun runFullGameAnalysis() {
        val state = _uiState.value
        if (state.moves.isEmpty() || state.isAnalyzing) return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = true,
                        analysisCurrent = 0,
                        analysisTotal = state.moves.size
                    )
                }
            }

            val (analyzedMoves, stats) = ChessAnalyzerEngine.analyzeFullGame(
                moves = state.moves,
                configuredDepth = state.settings.engineDepth,
                onProgress = { cur, tot ->
                    _uiState.update { it.copy(analysisCurrent = cur, analysisTotal = tot) }
                }
            )

            // Save analyzed game back to SQLite
            val pgnText = PgnParser.buildPgn(state.metadata, analyzedMoves)
            val record = SavedGameRecord(
                id = state.activeGameId.ifBlank { UUID.randomUUID().toString() },
                dedupKey = sqliteDb.computeDedupKey(state.metadata, analyzedMoves),
                createdAt = System.currentTimeMillis(),
                metadata = state.metadata,
                opening = state.opening,
                pgn = pgnText,
                finalFen = analyzedMoves.lastOrNull()?.fenAfter ?: INITIAL_FEN,
                statistics = stats,
                moves = analyzedMoves
            )
            sqliteDb.upsertGame(record)
            val allGames = sqliteDb.loadAllGames()
            val repertoire = buildRepertoireFromGames(allGames)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        activeGameId = record.id,
                        moves = analyzedMoves,
                        statistics = stats,
                        isAnalyzing = false,
                        savedGames = allGames,
                        repertoireOpenings = repertoire
                    )
                }
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _uiState.update { it.copy(isAnalyzing = false) }
    }

    fun flipBoard() {
        val nextSettings = _uiState.value.settings.copy(
            boardOrientationWhite = !_uiState.value.settings.boardOrientationWhite
        )
        updateSettings(nextSettings)
    }

    fun updateSettings(newSettings: AppSettings) {
        _uiState.update { it.copy(settings = newSettings) }
        viewModelScope.launch(Dispatchers.IO) {
            sqliteDb.saveSettings(newSettings)
        }
    }

    fun resetSettings() {
        updateSettings(AppSettings())
    }

    fun rebuildRepertoire() {
        viewModelScope.launch(Dispatchers.IO) {
            val allGames = sqliteDb.loadAllGames()
            val repertoire = buildRepertoireFromGames(allGames)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        savedGames = allGames,
                        repertoireOpenings = repertoire,
                        statusMessage = "Репертуар обновлён (${allGames.size} партий)"
                    )
                }
            }
        }
    }

    private fun buildRepertoireFromGames(games: List<SavedGameRecord>): List<RepertoireOpeningStat> {
        if (games.isEmpty()) return emptyList()
        val groupedByOpening = games.groupBy { "${it.opening.eco}|${it.opening.name}" }

        return groupedByOpening.map { (key, list) ->
            val first = list.first()
            val total = list.size.coerceAtLeast(1)
            val wins = list.count { it.metadata.result == "1-0" }
            val draws = list.count { it.metadata.result == "1/2-1/2" }
            val losses = list.count { it.metadata.result == "0-1" }

            val variations = list.groupBy {
                it.opening.variation.ifBlank { "Основная линия" }
            }.map { (varName, varGames) ->
                val vTotal = varGames.size.coerceAtLeast(1)
                val vWins = varGames.count { it.metadata.result == "1-0" }
                val vDraws = varGames.count { it.metadata.result == "1/2-1/2" }
                val vLosses = varGames.count { it.metadata.result == "0-1" }
                VariationStat(
                    variationName = varName,
                    gamesCount = varGames.size,
                    winRate = ((vWins * 100.0) / vTotal).roundToInt(),
                    drawRate = ((vDraws * 100.0) / vTotal).roundToInt(),
                    lossRate = ((vLosses * 100.0) / vTotal).roundToInt()
                )
            }.sortedByDescending { it.gamesCount }

            RepertoireOpeningStat(
                key = key,
                eco = first.opening.eco,
                openingName = first.opening.name,
                gamesCount = list.size,
                winRate = ((wins * 100.0) / total).roundToInt(),
                drawRate = ((draws * 100.0) / total).roundToInt(),
                lossRate = ((losses * 100.0) / total).roundToInt(),
                variations = variations
            )
        }.sortedByDescending { it.gamesCount }
    }
}
