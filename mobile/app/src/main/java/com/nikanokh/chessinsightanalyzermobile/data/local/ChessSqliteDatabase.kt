package com.nikanokh.chessinsightanalyzermobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.nikanokh.chessinsightanalyzermobile.domain.chess.EcoOpeningBook
import com.nikanokh.chessinsightanalyzermobile.domain.chess.PgnParser
import com.nikanokh.chessinsightanalyzermobile.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChessSqliteDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "chess_insight_analyzer_v4.db"
        private const val DB_VERSION = 1
        private const val TABLE_GAMES = "saved_games"
        private const val TABLE_SETTINGS = "app_settings"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_GAMES (
                id TEXT PRIMARY KEY NOT NULL,
                dedup_key TEXT UNIQUE NOT NULL,
                created_at INTEGER NOT NULL,
                white_name TEXT NOT NULL,
                black_name TEXT NOT NULL,
                white_rating TEXT NOT NULL,
                black_rating TEXT NOT NULL,
                result TEXT NOT NULL,
                game_date TEXT NOT NULL,
                event_name TEXT NOT NULL,
                site_name TEXT NOT NULL,
                platform TEXT NOT NULL,
                eco TEXT NOT NULL,
                opening_name TEXT NOT NULL,
                opening_variation TEXT NOT NULL,
                pgn TEXT NOT NULL,
                final_fen TEXT NOT NULL,
                stats_json TEXT,
                moves_json TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SETTINGS (
                setting_key TEXT PRIMARY KEY NOT NULL,
                setting_json TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun ensureDefaultMasterGamesSeeded() {
        val current = loadAllGames()
        if (current.isNotEmpty()) return

        for ((idx, sample) in EcoOpeningBook.sampleMasterGames.withIndex()) {
            val parsed = PgnParser.parsePgn(sample.pgn, "PGN")
            val record = SavedGameRecord(
                id = UUID.randomUUID().toString(),
                dedupKey = computeDedupKey(parsed.metadata, parsed.moves),
                createdAt = System.currentTimeMillis() - idx * 60_000L,
                metadata = parsed.metadata,
                opening = parsed.opening,
                pgn = parsed.rawPgn,
                finalFen = parsed.finalFen,
                statistics = null,
                moves = parsed.moves
            )
            upsertGame(record)
        }
    }

    fun computeDedupKey(metadata: GameMetadata, moves: List<MoveNode>): String {
        val sanPrefix = moves.take(30).joinToString(" ") { it.san }
        return "${metadata.whiteName.lowercase().trim()}|${metadata.blackName.lowercase().trim()}|${metadata.result}|$sanPrefix"
    }

    fun upsertGame(record: SavedGameRecord) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", record.id)
            put("dedup_key", record.dedupKey)
            put("created_at", record.createdAt)
            put("white_name", record.metadata.whiteName)
            put("black_name", record.metadata.blackName)
            put("white_rating", record.metadata.whiteRating)
            put("black_rating", record.metadata.blackRating)
            put("result", record.metadata.result)
            put("game_date", record.metadata.date)
            put("event_name", record.metadata.event)
            put("site_name", record.metadata.site)
            put("platform", record.metadata.platform)
            put("eco", record.opening.eco)
            put("opening_name", record.opening.name)
            put("opening_variation", record.opening.variation)
            put("pgn", record.pgn)
            put("final_fen", record.finalFen)
            put("stats_json", record.statistics?.let { serializeStats(it) })
            put("moves_json", serializeMoves(record.moves))
        }
        db.insertWithOnConflict(TABLE_GAMES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteGame(id: String) {
        writableDatabase.delete(TABLE_GAMES, "id = ?", arrayOf(id))
    }

    fun loadAllGames(): List<SavedGameRecord> {
        val list = mutableListOf<SavedGameRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GAMES,
            null,
            null,
            null,
            null,
            null,
            "created_at DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow("id"))
                val dedupKey = c.getString(c.getColumnIndexOrThrow("dedup_key"))
                val createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
                val metadata = GameMetadata(
                    whiteName = c.getString(c.getColumnIndexOrThrow("white_name")),
                    blackName = c.getString(c.getColumnIndexOrThrow("black_name")),
                    whiteRating = c.getString(c.getColumnIndexOrThrow("white_rating")),
                    blackRating = c.getString(c.getColumnIndexOrThrow("black_rating")),
                    result = c.getString(c.getColumnIndexOrThrow("result")),
                    date = c.getString(c.getColumnIndexOrThrow("game_date")),
                    event = c.getString(c.getColumnIndexOrThrow("event_name")),
                    site = c.getString(c.getColumnIndexOrThrow("site_name")),
                    eco = c.getString(c.getColumnIndexOrThrow("eco")),
                    platform = c.getString(c.getColumnIndexOrThrow("platform"))
                )
                val opening = OpeningInfo(
                    eco = c.getString(c.getColumnIndexOrThrow("eco")),
                    name = c.getString(c.getColumnIndexOrThrow("opening_name")),
                    variation = c.getString(c.getColumnIndexOrThrow("opening_variation"))
                )
                val pgn = c.getString(c.getColumnIndexOrThrow("pgn"))
                val finalFen = c.getString(c.getColumnIndexOrThrow("final_fen"))
                val statsJson = c.getString(c.getColumnIndexOrThrow("stats_json"))
                val movesJson = c.getString(c.getColumnIndexOrThrow("moves_json"))

                val moves = if (!movesJson.isNullOrBlank()) {
                    deserializeMoves(movesJson)
                } else {
                    PgnParser.parsePgn(pgn, metadata.platform).moves
                }

                list.add(
                    SavedGameRecord(
                        id = id,
                        dedupKey = dedupKey,
                        createdAt = createdAt,
                        metadata = metadata,
                        opening = opening,
                        pgn = pgn,
                        finalFen = finalFen,
                        statistics = statsJson?.let { deserializeStats(it) },
                        moves = moves
                    )
                )
            }
        }
        return list
    }

    fun saveSettings(settings: AppSettings) {
        val obj = JSONObject().apply {
            put("boardOrientationWhite", settings.boardOrientationWhite)
            put("showEvalBar", settings.showEvalBar)
            put("highlightLegalMoves", settings.highlightLegalMoves)
            put("showEngineArrows", settings.showEngineArrows)
            put("autoScrollMoves", settings.autoScrollMoves)
            put("engineDepth", settings.engineDepth)
            put("lightSquareHex", settings.lightSquareHex)
            put("darkSquareHex", settings.darkSquareHex)
        }
        val values = ContentValues().apply {
            put("setting_key", "main")
            put("setting_json", obj.toString())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_SETTINGS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun loadSettings(): AppSettings {
        val cursor = readableDatabase.query(
            TABLE_SETTINGS,
            arrayOf("setting_json"),
            "setting_key = ?",
            arrayOf("main"),
            null,
            null,
            null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                val raw = c.getString(0)
                return try {
                    val obj = JSONObject(raw)
                    AppSettings(
                        boardOrientationWhite = obj.optBoolean("boardOrientationWhite", true),
                        showEvalBar = obj.optBoolean("showEvalBar", true),
                        highlightLegalMoves = obj.optBoolean("highlightLegalMoves", true),
                        showEngineArrows = obj.optBoolean("showEngineArrows", true),
                        autoScrollMoves = obj.optBoolean("autoScrollMoves", true),
                        engineDepth = obj.optInt("engineDepth", 14),
                        lightSquareHex = obj.optLong("lightSquareHex", 0xFFEEEED2),
                        darkSquareHex = obj.optLong("darkSquareHex", 0xFF769656)
                    )
                } catch (_: Exception) {
                    AppSettings()
                }
            }
        }
        return AppSettings()
    }

    private fun serializeStats(stats: GameStatistics): String {
        val obj = JSONObject().apply {
            put("whiteAccuracy", stats.whiteAccuracy)
            put("blackAccuracy", stats.blackAccuracy)
            put("whiteAvgCpl", stats.whiteAvgCpl)
            put("blackAvgCpl", stats.blackAvgCpl)
            val wCounts = JSONObject()
            stats.whiteCategoryCounts.forEach { (k, v) -> wCounts.put(k.name, v) }
            put("whiteCategoryCounts", wCounts)
            val bCounts = JSONObject()
            stats.blackCategoryCounts.forEach { (k, v) -> bCounts.put(k.name, v) }
            put("blackCategoryCounts", bCounts)
        }
        return obj.toString()
    }

    private fun deserializeStats(raw: String): GameStatistics? {
        return try {
            val obj = JSONObject(raw)
            val wMap = mutableMapOf<MoveCategory, Int>()
            obj.optJSONObject("whiteCategoryCounts")?.let { wObj ->
                for (key in wObj.keys()) {
                    runCatching { MoveCategory.valueOf(key) }.getOrNull()?.let { cat ->
                        wMap[cat] = wObj.optInt(key, 0)
                    }
                }
            }
            val bMap = mutableMapOf<MoveCategory, Int>()
            obj.optJSONObject("blackCategoryCounts")?.let { bObj ->
                for (key in bObj.keys()) {
                    runCatching { MoveCategory.valueOf(key) }.getOrNull()?.let { cat ->
                        bMap[cat] = bObj.optInt(key, 0)
                    }
                }
            }
            GameStatistics(
                whiteAccuracy = obj.optDouble("whiteAccuracy", 0.0),
                blackAccuracy = obj.optDouble("blackAccuracy", 0.0),
                whiteAvgCpl = obj.optInt("whiteAvgCpl", 0),
                blackAvgCpl = obj.optInt("blackAvgCpl", 0),
                whiteCategoryCounts = wMap,
                blackCategoryCounts = bMap
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeMoves(moves: List<MoveNode>): String {
        val arr = JSONArray()
        for (m in moves) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("ply", m.ply)
                put("moveNumber", m.moveNumber)
                put("color", m.color.name)
                put("san", m.san)
                put("uci", m.uci)
                put("fromSquare", m.fromSquare)
                put("toSquare", m.toSquare)
                put("fenBefore", m.fenBefore)
                put("fenAfter", m.fenAfter)
                m.analysis?.let { a ->
                    put(
                        "analysis",
                        JSONObject().apply {
                            put("evalBeforeCp", a.evalBeforeCp)
                            put("evalAfterCp", a.evalAfterCp)
                            if (a.mateAfter != null) put("mateAfter", a.mateAfter)
                            put("cpl", a.cpl)
                            put("bestMoveUci", a.bestMoveUci)
                            put("bestMoveSan", a.bestMoveSan)
                            put("category", a.category.name)
                        }
                    )
                }
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeMoves(raw: String): List<MoveNode> {
        val result = mutableListOf<MoveNode>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val analysisObj = obj.optJSONObject("analysis")
                val analysis = analysisObj?.let { a ->
                    MoveAnalysis(
                        evalBeforeCp = a.optInt("evalBeforeCp", 0),
                        evalAfterCp = a.optInt("evalAfterCp", 0),
                        mateAfter = if (a.has("mateAfter")) a.optInt("mateAfter") else null,
                        cpl = a.optInt("cpl", 0),
                        bestMoveUci = a.optString("bestMoveUci", ""),
                        bestMoveSan = a.optString("bestMoveSan", ""),
                        category = runCatching {
                            MoveCategory.valueOf(a.optString("category", "GOOD"))
                        }.getOrDefault(MoveCategory.GOOD)
                    )
                }
                result.add(
                    MoveNode(
                        id = obj.optString("id", "ply_${i + 1}"),
                        ply = obj.optInt("ply", i + 1),
                        moveNumber = obj.optInt("moveNumber", (i / 2) + 1),
                        color = if (obj.optString("color") == "BLACK") PieceColor.BLACK else PieceColor.WHITE,
                        san = obj.optString("san", ""),
                        uci = obj.optString("uci", ""),
                        fromSquare = obj.optString("fromSquare", ""),
                        toSquare = obj.optString("toSquare", ""),
                        fenBefore = obj.optString("fenBefore", ""),
                        fenAfter = obj.optString("fenAfter", ""),
                        analysis = analysis
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }
}
