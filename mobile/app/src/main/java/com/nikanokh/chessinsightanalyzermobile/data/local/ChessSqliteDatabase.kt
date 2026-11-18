package com.nikanokh.chessinsightanalyzermobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.nikanokh.chessinsightanalyzermobile.data.remote.OnlineImportService
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessEngine
import com.nikanokh.chessinsightanalyzermobile.domain.model.AppSettings
import com.nikanokh.chessinsightanalyzermobile.domain.model.BOARD_THEME_PRESETS
import com.nikanokh.chessinsightanalyzermobile.domain.model.ClassificationThresholds
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineConfig
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineEvaluation
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineLine
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveAnalysis
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.RepertoireEntry
import com.nikanokh.chessinsightanalyzermobile.domain.model.SavedGameRecord
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChessSqliteDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "chess_insight_analyzer.db"
        private const val DB_VERSION = 3

        private const val TABLE_GAMES = "saved_games"
        private const val TABLE_REPERTOIRE = "opening_repertoire"
        private const val TABLE_SETTINGS = "app_settings"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_GAMES (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                white TEXT NOT NULL,
                black TEXT NOT NULL,
                white_elo TEXT NOT NULL,
                black_elo TEXT NOT NULL,
                result TEXT NOT NULL,
                event TEXT NOT NULL,
                date TEXT NOT NULL,
                eco TEXT NOT NULL,
                opening_name TEXT NOT NULL,
                pgn TEXT NOT NULL,
                initial_fen TEXT NOT NULL,
                moves_count INTEGER NOT NULL,
                white_accuracy REAL,
                black_accuracy REAL,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                folder_tag TEXT NOT NULL DEFAULT 'Общие',
                created_at INTEGER NOT NULL,
                analyses_json TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_REPERTOIRE (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                color TEXT NOT NULL,
                eco TEXT NOT NULL,
                pgn_moves TEXT NOT NULL,
                target_fen TEXT NOT NULL,
                notes TEXT NOT NULL,
                tags TEXT NOT NULL,
                practiced_count INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0,
                last_practiced_at INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SETTINGS (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )

        seedDefaultData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GAMES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REPERTOIRE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        onCreate(db)
    }

    private fun seedDefaultData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        for ((idx, sample) in OnlineImportService.SAMPLE_GAMES.withIndex()) {
            val parsed = ChessEngine.parsePgn(sample.pgn)
            val cv = ContentValues().apply {
                put("id", "sample_${idx + 1}")
                put("title", sample.title)
                put("white", parsed.headers.white)
                put("black", parsed.headers.black)
                put("white_elo", parsed.headers.whiteElo)
                put("black_elo", parsed.headers.blackElo)
                put("result", parsed.headers.result)
                put("event", parsed.headers.event)
                put("date", parsed.headers.date)
                put("eco", parsed.headers.eco)
                put("opening_name", parsed.headers.opening)
                put("pgn", sample.pgn)
                put("initial_fen", parsed.initialFen)
                put("moves_count", parsed.nodes.size)
                put("is_favorite", 1)
                put("folder_tag", "Шедевры")
                put("created_at", now - idx * 60000L)
            }
            db.insertWithOnConflict(TABLE_GAMES, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }

        val defaultRepertoire = listOf(
            RepertoireEntry(
                id = "rep_italian",
                title = "Итальянская партия (Giuoco Piano)",
                color = PieceColor.WHITE,
                eco = "C53",
                pgnMoves = "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. c3 Nf6 5. d3",
                targetFen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2PP1N2/PP3PPP/RNBQK2R b KQkq - 0 5",
                notes = "План белых: укрепить центр c3-d4, вывести слона на e3 и коня на d2-f1-g3.",
                tags = "Открытые, Белые, C53",
                practicedCount = 5,
                correctCount = 5,
                lastPracticedAt = now
            ),
            RepertoireEntry(
                id = "rep_najdorf",
                title = "Сицилианская защита: Вариант Найдорфа",
                color = PieceColor.BLACK,
                eco = "B90",
                pgnMoves = "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6",
                targetFen = "rnbqkb1r/1p2pppp/p2p1n2/8/3NP3/2N5/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                notes = "Контроль поля b5, подготовка контригры e5 или e6 с давлением по линии c.",
                tags = "Сицилианская, Чёрные, B90",
                practicedCount = 8,
                correctCount = 7,
                lastPracticedAt = now
            ),
            RepertoireEntry(
                id = "rep_london",
                title = "Лондонская система",
                color = PieceColor.WHITE,
                eco = "D02",
                pgnMoves = "1. d4 d5 2. Nf3 Nf6 3. Bf4",
                targetFen = "rnbqkb1r/ppp1pppp/5n2/3p4/3P1B2/5N2/PPP1PPPP/RN1QKB1R b KQkq - 1 3",
                notes = "Надёжная пирамида пешек c3-d4-e3, контроль поля e5 и атака на королевском фланге.",
                tags = "Закрытые, Белые, D02",
                practicedCount = 4,
                correctCount = 4,
                lastPracticedAt = now
            )
        )

        for (rep in defaultRepertoire) {
            val cv = ContentValues().apply {
                put("id", rep.id)
                put("title", rep.title)
                put("color", rep.color.code.toString())
                put("eco", rep.eco)
                put("pgn_moves", rep.pgnMoves)
                put("target_fen", rep.targetFen)
                put("notes", rep.notes)
                put("tags", rep.tags)
                put("practiced_count", rep.practicedCount)
                put("correct_count", rep.correctCount)
                put("last_practiced_at", rep.lastPracticedAt)
            }
            db.insertWithOnConflict(TABLE_REPERTOIRE, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun getAllSavedGames(): List<SavedGameRecord> {
        val list = mutableListOf<SavedGameRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_GAMES ORDER BY created_at DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val wAccIdx = c.getColumnIndexOrThrow("white_accuracy")
                val bAccIdx = c.getColumnIndexOrThrow("black_accuracy")
                val analysesIdx = c.getColumnIndex("analyses_json")
                list.add(
                    SavedGameRecord(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        white = c.getString(c.getColumnIndexOrThrow("white")),
                        black = c.getString(c.getColumnIndexOrThrow("black")),
                        whiteElo = c.getString(c.getColumnIndexOrThrow("white_elo")),
                        blackElo = c.getString(c.getColumnIndexOrThrow("black_elo")),
                        result = c.getString(c.getColumnIndexOrThrow("result")),
                        event = c.getString(c.getColumnIndexOrThrow("event")),
                        date = c.getString(c.getColumnIndexOrThrow("date")),
                        eco = c.getString(c.getColumnIndexOrThrow("eco")),
                        openingName = c.getString(c.getColumnIndexOrThrow("opening_name")),
                        pgn = c.getString(c.getColumnIndexOrThrow("pgn")),
                        initialFen = c.getString(c.getColumnIndexOrThrow("initial_fen")),
                        movesCount = c.getInt(c.getColumnIndexOrThrow("moves_count")),
                        whiteAccuracy = if (c.isNull(wAccIdx)) null else c.getDouble(wAccIdx),
                        blackAccuracy = if (c.isNull(bAccIdx)) null else c.getDouble(bAccIdx),
                        isFavorite = c.getInt(c.getColumnIndexOrThrow("is_favorite")) == 1,
                        folderTag = c.getString(c.getColumnIndexOrThrow("folder_tag")),
                        createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                        analysesJson = if (analysesIdx != -1 && !c.isNull(analysesIdx)) c.getString(analysesIdx) else null
                    )
                )
            }
        }
        return list
    }

    fun saveGame(record: SavedGameRecord) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", record.id)
            put("title", record.title)
            put("white", record.white)
            put("black", record.black)
            put("white_elo", record.whiteElo)
            put("black_elo", record.blackElo)
            put("result", record.result)
            put("event", record.event)
            put("date", record.date)
            put("eco", record.eco)
            put("opening_name", record.openingName)
            put("pgn", record.pgn)
            put("initial_fen", record.initialFen)
            put("moves_count", record.movesCount)
            if (record.whiteAccuracy != null) put("white_accuracy", record.whiteAccuracy) else putNull("white_accuracy")
            if (record.blackAccuracy != null) put("black_accuracy", record.blackAccuracy) else putNull("black_accuracy")
            put("is_favorite", if (record.isFavorite) 1 else 0)
            put("folder_tag", record.folderTag)
            put("created_at", record.createdAt)
            if (record.analysesJson != null) put("analyses_json", record.analysesJson) else putNull("analyses_json")
        }
        db.insertWithOnConflict(TABLE_GAMES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun toggleFavoriteGame(id: String, isFav: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_favorite", if (isFav) 1 else 0)
        }
        db.update(TABLE_GAMES, cv, "id = ?", arrayOf(id))
    }

    fun deleteGame(id: String) {
        writableDatabase.delete(TABLE_GAMES, "id = ?", arrayOf(id))
    }

    fun getAllRepertoire(): List<RepertoireEntry> {
        val list = mutableListOf<RepertoireEntry>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_REPERTOIRE ORDER BY eco ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val lastIdx = c.getColumnIndexOrThrow("last_practiced_at")
                list.add(
                    RepertoireEntry(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        color = PieceColor.fromCode(c.getString(c.getColumnIndexOrThrow("color")).firstOrNull() ?: 'w'),
                        eco = c.getString(c.getColumnIndexOrThrow("eco")),
                        pgnMoves = c.getString(c.getColumnIndexOrThrow("pgn_moves")),
                        targetFen = c.getString(c.getColumnIndexOrThrow("target_fen")),
                        notes = c.getString(c.getColumnIndexOrThrow("notes")),
                        tags = c.getString(c.getColumnIndexOrThrow("tags")),
                        practicedCount = c.getInt(c.getColumnIndexOrThrow("practiced_count")),
                        correctCount = c.getInt(c.getColumnIndexOrThrow("correct_count")),
                        lastPracticedAt = if (c.isNull(lastIdx)) null else c.getLong(lastIdx)
                    )
                )
            }
        }
        return list
    }

    fun saveRepertoireEntry(entry: RepertoireEntry) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", entry.id.ifEmpty { UUID.randomUUID().toString() })
            put("title", entry.title)
            put("color", entry.color.code.toString())
            put("eco", entry.eco)
            put("pgn_moves", entry.pgnMoves)
            put("target_fen", entry.targetFen)
            put("notes", entry.notes)
            put("tags", entry.tags)
            put("practiced_count", entry.practicedCount)
            put("correct_count", entry.correctCount)
            put("last_practiced_at", entry.lastPracticedAt)
        }
        db.insertWithOnConflict(TABLE_REPERTOIRE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteRepertoireEntry(id: String) {
        writableDatabase.delete(TABLE_REPERTOIRE, "id = ?", arrayOf(id))
    }

    fun loadSettings(): AppSettings {
        val db = readableDatabase
        val map = mutableMapOf<String, String>()
        val cursor = db.rawQuery("SELECT key, value FROM $TABLE_SETTINGS", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                map[c.getString(0)] = c.getString(1)
            }
        }

        val themeId = map["boardThemeId"] ?: "classic_green"
        val preset = BOARD_THEME_PRESETS.firstOrNull { it.id == themeId } ?: BOARD_THEME_PRESETS.first()

        return AppSettings(
            boardThemeId = themeId,
            lightSquareColorHex = map["lightSquareColorHex"]?.toLongOrNull() ?: preset.lightSquareHex,
            darkSquareColorHex = map["darkSquareColorHex"]?.toLongOrNull() ?: preset.darkSquareHex,
            pieceSet = map["pieceSet"] ?: "neo",
            boardSize = map["boardSize"] ?: "auto",
            enableAnimations = map["enableAnimations"]?.toBooleanStrictOrNull() ?: true,
            highlightLastMove = map["highlightLastMove"]?.toBooleanStrictOrNull() ?: true,
            highlightLegalMoves = map["highlightLegalMoves"]?.toBooleanStrictOrNull() ?: true,
            showCoordinates = map["showCoordinates"]?.toBooleanStrictOrNull() ?: true,
            showEvalBar = map["showEvalBar"]?.toBooleanStrictOrNull() ?: true,
            showLeftSidebar = map["showLeftSidebar"]?.toBooleanStrictOrNull() ?: true,
            showUserArrows = map["showUserArrows"]?.toBooleanStrictOrNull() ?: true,
            showEngineArrows = map["showEngineArrows"]?.toBooleanStrictOrNull() ?: true,
            userArrowColorHex = map["userArrowColorHex"] ?: "#F59E0B",
            engineArrowColorHex = map["engineArrowColorHex"] ?: "#10B981",
            secondaryEngineArrowColorHex = map["secondaryEngineArrowColorHex"] ?: "#3B82F6",
            arrowWidth = map["arrowWidth"]?.toIntOrNull()?.coerceIn(14, 32) ?: 20,
            arrowOpacity = map["arrowOpacity"]?.toFloatOrNull()?.coerceIn(0.4f, 1.0f) ?: 0.85f,
            engineConfig = EngineConfig(
                depth = map["engineDepth"]?.toIntOrNull()?.coerceIn(1, 30) ?: 18,
                multiPv = map["multiPv"]?.toIntOrNull()?.coerceIn(1, 5) ?: 3,
                threads = map["engineThreads"]?.toIntOrNull()?.coerceIn(1, 4) ?: 2,
                hashMb = map["engineHashMb"]?.toIntOrNull()?.coerceIn(16, 512) ?: 64,
                movetimeMs = map["movetimeMs"]?.toIntOrNull()?.takeIf { it > 0 },
                skillLevel = map["skillLevel"]?.toIntOrNull()?.coerceIn(0, 20) ?: 20
            ),
            thresholds = ClassificationThresholds(
                bestMaxCpl = map["bestMaxCpl"]?.toIntOrNull() ?: 20,
                excellentMaxCpl = map["excellentMaxCpl"]?.toIntOrNull() ?: 50,
                goodMaxCpl = map["goodMaxCpl"]?.toIntOrNull() ?: 95,
                inaccuracyMaxCpl = map["inaccuracyMaxCpl"]?.toIntOrNull() ?: 175,
                mistakeMaxCpl = map["mistakeMaxCpl"]?.toIntOrNull() ?: 300
            )
        )
    }

    fun saveSettings(settings: AppSettings) {
        val db = writableDatabase
        val pairs = mapOf(
            "boardThemeId" to settings.boardThemeId,
            "lightSquareColorHex" to settings.lightSquareColorHex.toString(),
            "darkSquareColorHex" to settings.darkSquareColorHex.toString(),
            "pieceSet" to settings.pieceSet,
            "boardSize" to settings.boardSize,
            "enableAnimations" to settings.enableAnimations.toString(),
            "highlightLastMove" to settings.highlightLastMove.toString(),
            "highlightLegalMoves" to settings.highlightLegalMoves.toString(),
            "showCoordinates" to settings.showCoordinates.toString(),
            "showEvalBar" to settings.showEvalBar.toString(),
            "showLeftSidebar" to settings.showLeftSidebar.toString(),
            "showUserArrows" to settings.showUserArrows.toString(),
            "showEngineArrows" to settings.showEngineArrows.toString(),
            "userArrowColorHex" to settings.userArrowColorHex,
            "engineArrowColorHex" to settings.engineArrowColorHex,
            "secondaryEngineArrowColorHex" to settings.secondaryEngineArrowColorHex,
            "arrowWidth" to settings.arrowWidth.toString(),
            "arrowOpacity" to settings.arrowOpacity.toString(),
            "engineDepth" to settings.engineConfig.depth.toString(),
            "multiPv" to settings.engineConfig.multiPv.toString(),
            "engineThreads" to settings.engineConfig.threads.toString(),
            "engineHashMb" to settings.engineConfig.hashMb.toString(),
            "movetimeMs" to (settings.engineConfig.movetimeMs ?: 0).toString(),
            "skillLevel" to settings.engineConfig.skillLevel.toString(),
            "bestMaxCpl" to settings.thresholds.bestMaxCpl.toString(),
            "excellentMaxCpl" to settings.thresholds.excellentMaxCpl.toString(),
            "goodMaxCpl" to settings.thresholds.goodMaxCpl.toString(),
            "inaccuracyMaxCpl" to settings.thresholds.inaccuracyMaxCpl.toString(),
            "mistakeMaxCpl" to settings.thresholds.mistakeMaxCpl.toString()
        )
        db.beginTransaction()
        try {
            for ((k, v) in pairs) {
                val cv = ContentValues().apply {
                    put("key", k)
                    put("value", v)
                }
                db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun serializeAnalyses(analyses: List<MoveAnalysis>): String {
        val arr = JSONArray()
        for (a in analyses) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("ply", a.ply)
                put("moveNumber", a.moveNumber)
                put("color", a.color.code.toString())
                put("san", a.san)
                put("uci", a.uci)
                put("from", a.from)
                put("to", a.to)
                put("piece", a.piece.toString())
                if (a.captured != null) put("captured", a.captured.toString())
                if (a.promotion != null) put("promotion", a.promotion.toString())
                put("fenBefore", a.fenBefore)
                put("fenAfter", a.fenAfter)
                put("isBook", a.isBook)
                if (a.openingEco != null) put("openingEco", a.openingEco)
                if (a.openingName != null) put("openingName", a.openingName)
                put("legalMovesCount", a.legalMovesCount)
                if (a.evalBefore != null) {
                    put("evalBeforeCp", a.evalBefore.cp)
                    put("evalBeforeMate", a.evalBefore.mate)
                    put("evalBeforeDepth", a.evalBefore.depth)
                }
                if (a.evalAfter != null) {
                    put("evalAfterCp", a.evalAfter.cp)
                    put("evalAfterMate", a.evalAfter.mate)
                    put("evalAfterDepth", a.evalAfter.depth)
                }
                put("bestMoveUci", a.bestMoveUci ?: "")
                put("bestMoveSan", a.bestMoveSan ?: "")
                put("cpLoss", a.cpLoss)
                put("winProbBefore", a.winProbBefore)
                put("winProbAfter", a.winProbAfter)
                put("winProbLoss", a.winProbLoss)
                put("accuracy", a.accuracy)
                put("category", a.category.id)
                put("isSacrifice", a.isSacrifice)
                put("isOnlyBestMove", a.isOnlyBestMove)
                put("isMissedTactic", a.isMissedTactic)
                put("explanation", a.explanation)
                val linesArr = JSONArray()
                for (l in a.topLines) {
                    linesArr.put(
                        JSONObject().apply {
                            put("multipv", l.multipv)
                            put("depth", l.depth)
                            if (l.cp != null) put("cp", l.cp)
                            if (l.mate != null) put("mate", l.mate)
                            put("bestMoveUci", l.bestMoveUci)
                            put("bestMoveSan", l.bestMoveSan)
                            put("pvUci", JSONArray(l.pvUci))
                            put("pvSan", JSONArray(l.pvSan))
                        }
                    )
                }
                put("topLines", linesArr)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeAnalyses(rawJson: String?): List<MoveAnalysis> {
        if (rawJson.isNullOrBlank()) return emptyList()
        val result = mutableListOf<MoveAnalysis>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val evalBefore = if (obj.has("evalBeforeCp") || obj.has("evalBeforeMate")) {
                    EngineEvaluation(
                        cp = if (obj.has("evalBeforeCp") && !obj.isNull("evalBeforeCp")) obj.getInt("evalBeforeCp") else null,
                        mate = if (obj.has("evalBeforeMate") && !obj.isNull("evalBeforeMate")) obj.getInt("evalBeforeMate") else null,
                        depth = obj.optInt("evalBeforeDepth", 18)
                    )
                } else null
                val evalAfter = if (obj.has("evalAfterCp") || obj.has("evalAfterMate")) {
                    EngineEvaluation(
                        cp = if (obj.has("evalAfterCp") && !obj.isNull("evalAfterCp")) obj.getInt("evalAfterCp") else null,
                        mate = if (obj.has("evalAfterMate") && !obj.isNull("evalAfterMate")) obj.getInt("evalAfterMate") else null,
                        depth = obj.optInt("evalAfterDepth", 18)
                    )
                } else null

                val linesList = mutableListOf<EngineLine>()
                val linesArr = obj.optJSONArray("topLines")
                if (linesArr != null) {
                    for (j in 0 until linesArr.length()) {
                        val lObj = linesArr.getJSONObject(j)
                        val pvUciArr = lObj.optJSONArray("pvUci")
                        val pvSanArr = lObj.optJSONArray("pvSan")
                        val pvUci = mutableListOf<String>()
                        val pvSan = mutableListOf<String>()
                        if (pvUciArr != null) {
                            for (k in 0 until pvUciArr.length()) pvUci.add(pvUciArr.optString(k))
                        }
                        if (pvSanArr != null) {
                            for (k in 0 until pvSanArr.length()) pvSan.add(pvSanArr.optString(k))
                        }
                        linesList.add(
                            EngineLine(
                                multipv = lObj.optInt("multipv", j + 1),
                                depth = lObj.optInt("depth", 18),
                                cp = if (lObj.has("cp") && !lObj.isNull("cp")) lObj.getInt("cp") else null,
                                mate = if (lObj.has("mate") && !lObj.isNull("mate")) lObj.getInt("mate") else null,
                                pvUci = pvUci,
                                pvSan = pvSan,
                                bestMoveUci = lObj.optString("bestMoveUci", ""),
                                bestMoveSan = lObj.optString("bestMoveSan", "")
                            )
                        )
                    }
                }

                result.add(
                    MoveAnalysis(
                        id = obj.optString("id", "ply_${i + 1}"),
                        ply = obj.optInt("ply", i + 1),
                        moveNumber = obj.optInt("moveNumber", (i / 2) + 1),
                        color = PieceColor.fromCode(obj.optString("color", "w").firstOrNull() ?: 'w'),
                        san = obj.optString("san", ""),
                        uci = obj.optString("uci", ""),
                        from = obj.optString("from", ""),
                        to = obj.optString("to", ""),
                        piece = obj.optString("piece", "p").firstOrNull() ?: 'p',
                        captured = obj.optString("captured", "").firstOrNull(),
                        promotion = obj.optString("promotion", "").firstOrNull(),
                        fenBefore = obj.optString("fenBefore", ChessEngine.INITIAL_FEN),
                        fenAfter = obj.optString("fenAfter", ChessEngine.INITIAL_FEN),
                        isBook = obj.optBoolean("isBook", false),
                        openingEco = obj.optString("openingEco", "").takeIf { it.isNotBlank() },
                        openingName = obj.optString("openingName", "").takeIf { it.isNotBlank() },
                        legalMovesCount = obj.optInt("legalMovesCount", 20),
                        evalBefore = evalBefore,
                        evalAfter = evalAfter,
                        bestMoveUci = obj.optString("bestMoveUci", ""),
                        bestMoveSan = obj.optString("bestMoveSan", ""),
                        topLines = linesList,
                        cpLoss = obj.optInt("cpLoss", 0),
                        winProbBefore = obj.optDouble("winProbBefore", 50.0),
                        winProbAfter = obj.optDouble("winProbAfter", 50.0),
                        winProbLoss = obj.optDouble("winProbLoss", 0.0),
                        accuracy = obj.optDouble("accuracy", 100.0),
                        category = MoveCategory.fromId(obj.optString("category", "Good")),
                        isSacrifice = obj.optBoolean("isSacrifice", false),
                        isOnlyBestMove = obj.optBoolean("isOnlyBestMove", false),
                        isMissedTactic = obj.optBoolean("isMissedTactic", false),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }
}
