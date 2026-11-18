package com.nikanokh.chessinsightanalyzermobile.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object OnlineImportService {

    suspend fun fetchLichessGamesPgn(username: String, maxGames: Int = 10): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanUser = username.trim()
                require(cleanUser.isNotEmpty()) { "Введите имя пользователя Lichess" }
                val url = URL("https://lichess.org/api/games/user/$cleanUser?max=$maxGames&opening=true")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/x-chess-pgn")
                    setRequestProperty("User-Agent", "ChessInsightAnalyzerAndroid/4.0")
                    connectTimeout = 10_000
                    readTimeout = 12_000
                }
                if (conn.responseCode != 200) {
                    throw IllegalStateException("Lichess вернул код ${conn.responseCode}")
                }
                val rawText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                splitMultiPgn(rawText)
            }
        }

    suspend fun fetchChessComGamesPgn(username: String, maxGames: Int = 10): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanUser = username.trim().lowercase()
                require(cleanUser.isNotEmpty()) { "Введите имя пользователя Chess.com" }
                val archivesUrl = URL("https://api.chess.com/pub/player/$cleanUser/games/archives")
                val archivesConn = (archivesUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "ChessInsightAnalyzerAndroid/4.0")
                    connectTimeout = 10_000
                    readTimeout = 12_000
                }
                if (archivesConn.responseCode != 200) {
                    throw IllegalStateException("Игрок '$cleanUser' не найден на Chess.com")
                }
                val archivesJson = BufferedReader(InputStreamReader(archivesConn.inputStream)).use { it.readText() }
                val archivesArr = JSONObject(archivesJson).optJSONArray("archives")
                if (archivesArr == null || archivesArr.length() == 0) {
                    return@runCatching emptyList()
                }
                val latestArchiveUrl = archivesArr.getString(archivesArr.length() - 1)
                val gamesConn = (URL(latestArchiveUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "ChessInsightAnalyzerAndroid/4.0")
                    connectTimeout = 10_000
                    readTimeout = 12_000
                }
                val gamesJson = BufferedReader(InputStreamReader(gamesConn.inputStream)).use { it.readText() }
                val gamesArr = JSONObject(gamesJson).optJSONArray("games") ?: return@runCatching emptyList()
                val pgns = mutableListOf<String>()
                for (i in (gamesArr.length() - 1) downTo 0) {
                    val pgn = gamesArr.getJSONObject(i).optString("pgn", "")
                    if (pgn.isNotBlank()) {
                        pgns.add(pgn)
                        if (pgns.size >= maxGames) break
                    }
                }
                pgns
            }
        }

    fun splitMultiPgn(rawText: String): List<String> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()
        val chunks = trimmed.split(Regex("""(?=\[Event\s+")"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (chunks.isEmpty()) listOf(trimmed) else chunks
    }
}
