package com.nikanokh.chessinsightanalyzermobile.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ChessComProfile(
    val username: String,
    val avatar: String? = null,
    val title: String? = null,
    val country: String? = null,
    val followers: Int? = null,
    val status: String? = null,
    val url: String? = null
)

data class ChessComPlayerInfo(
    val username: String,
    val rating: Int,
    val result: String
)

data class ChessComGameItem(
    val url: String,
    val pgn: String,
    val timeControl: String,
    val endTime: Long,
    val rated: Boolean,
    val timeClass: String,
    val rules: String,
    val white: ChessComPlayerInfo,
    val black: ChessComPlayerInfo,
    val uuid: String? = null
)

data class ChessComOutcomeInfo(
    val labelRu: String,
    val outcome: String // "win" | "draw" | "loss"
)

data class SampleMasterGame(
    val title: String,
    val subtitle: String,
    val eco: String,
    val pgn: String
)

/**
 * 1:1 port of Web `src/services/ChessComApi/chessComService.ts` + Lichess PGN API.
 * Includes:
 * - Exponential backoff retry on HTTP 429 / 5xx
 * - 2-minute TTL response cache
 * - Full profile, archive list, monthly archive batching, and specific Year/Month loading
 * - Stable deduplication (`getChessComGameKey`, `mergeAndDeduplicateChessComGames`)
 * - Complete Chess.com result mapping (`mapChessComResult`)
 */
object OnlineImportService {

    private const val CHESS_COM_BASE = "https://api.chess.com/pub"
    private const val CACHE_TTL_MS = 2 * 60 * 1000L

    private data class CacheEntry(val data: String, val expiresAt: Long)
    private val responseCache = HashMap<String, CacheEntry>()

    val QUICK_GM_PRESETS = listOf(
        "Hikaru",
        "MagnusCarlsen",
        "DanielNaroditsky",
        "GothamChess"
    )

    val SAMPLE_GAMES: List<SampleMasterGame> = listOf(
        SampleMasterGame(
            title = "Paul Morphy vs Duke Karl / Count Isouard (1858)",
            subtitle = "Оперная партия в Париже — классическая жертва ладьи и ферзя",
            eco = "C41",
            pgn = """
                [Event "Paris Opera"]
                [Site "Paris FRA"]
                [Date "1858.11.02"]
                [Round "1"]
                [White "Paul Morphy"]
                [Black "Duke Karl / Count Isouard"]
                [Result "1-0"]
                [WhiteElo "2740"]
                [BlackElo "1950"]
                [ECO "C41"]
                [Opening "Защита Филидора"]

                1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7 8. Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0
            """.trimIndent()
        ),
        SampleMasterGame(
            title = "Garry Kasparov vs Veselin Topalov (1999)",
            subtitle = "Бессмертная партия Каспарова в Вейк-ан-Зее (жертва ладьи на d4)",
            eco = "B07",
            pgn = """
                [Event "Hoogovens Wijk aan Zee"]
                [Site "Wijk aan Zee NED"]
                [Date "1999.01.20"]
                [Round "4"]
                [White "Garry Kasparov"]
                [Black "Veselin Topalov"]
                [Result "1-0"]
                [WhiteElo "2812"]
                [BlackElo "2700"]
                [ECO "B07"]
                [Opening "Защита Пирца-Уфимцева"]

                1. e4 d6 2. d4 Nf6 3. Nc3 g6 4. Be3 Bg7 5. Qd2 c6 6. f3 b5 7. Nge2 Nbd7 8. Bh6 Bxh6 9. Qxh6 Bb7 10. a3 e5 11. O-O-O Qe7 12. Kb1 a6 13. Nc1 O-O-O 14. Nb3 exd4 15. Rxd4 c5 16. Rd1 Nb6 17. g3 Kb8 18. Na5 Ba8 19. Bh3 d5 20. Qf4+ Ka7 21. Rhe1 d4 22. Nd5 Nbxd5 23. exd5 Qd6 24. Rxd4 cxd4 25. Re7+ Kb6 26. Qxd4+ Kxa5 27. b4+ Ka4 28. Qc3 Qxd5 29. Ra7 Bb7 30. Rxb7 Qc4 31. Qxf6 Kxa3 32. Qxa6+ Kxb4 33. c3+ Kxc3 34. Qa1+ Kd2 35. Qb2+ Kd1 36. Bf1 Rd2 37. Rd7 Rxd7 38. Bxc4 bxc4 39. Qxh8 Rd3 40. Qa8 c3 41. Qa4+ Ke1 42. f4 f5 43. Kc1 Rd2 44. Qa7 1-0
            """.trimIndent()
        ),
        SampleMasterGame(
            title = "Adolf Anderssen vs Lionel Kieseritzky (1851)",
            subtitle = "Бессмертная партия — жертва слона, обеих ладей и ферзя",
            eco = "C33",
            pgn = """
                [Event "Immortal Game"]
                [Site "London ENG"]
                [Date "1851.06.21"]
                [Round "1"]
                [White "Adolf Anderssen"]
                [Black "Lionel Kieseritzky"]
                [Result "1-0"]
                [WhiteElo "2600"]
                [BlackElo "2450"]
                [ECO "C33"]
                [Opening "Королевский гамбит"]

                1. e4 e5 2. f4 exf4 3. Bc4 Qh4+ 4. Kf1 b5 5. Bxb5 Nf6 6. Nf3 Qh6 7. d3 Nh5 8. Nh4 Qg5 9. Nf5 c6 10. g4 Nf6 11. Rg1 cxb5 12. h4 Qg6 13. h5 Qg5 14. Qf3 Ng8 15. Bxf4 Qf6 16. Nc3 Bc5 17. Nd5 Qxb2 18. Bd6 Bxg1 19. e5 Qxa1+ 20. Ke2 Na6 21. Nxg7+ Kd8 22. Qf6+ Nxf6 23. Be7# 1-0
            """.trimIndent()
        )
    )

    private suspend fun fetchJsonWithRetry(
        urlStr: String,
        notFoundMessage: String,
        maxRetries: Int = 2
    ): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        synchronized(responseCache) {
            val cached = responseCache[urlStr]
            if (cached != null && cached.expiresAt > now) {
                return@withContext cached.data
            }
        }

        var lastError: Exception? = null
        for (attempt in 0..maxRetries) {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty(
                    "User-Agent",
                    "ChessInsightAnalyzer/3.0 (Android Jetpack Compose; https://chess.com)"
                )
            }
            try {
                val code = conn.responseCode
                if (code == 404) {
                    throw IllegalArgumentException(notFoundMessage)
                }
                if (code == 429 || code >= 500) {
                    if (attempt < maxRetries) {
                        delay(450L * (attempt + 1))
                        continue
                    }
                    if (code == 429) {
                        throw IllegalStateException("Превышен лимит запросов к API Chess.com (HTTP 429). Подождите несколько секунд и повторите попытку.")
                    }
                    throw IllegalStateException("Сервер Chess.com временно недоступен (HTTP $code).")
                }
                if (code !in 200..299) {
                    throw IllegalStateException("Ошибка ответа Chess.com API: HTTP $code")
                }

                val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                synchronized(responseCache) {
                    responseCache[urlStr] = CacheEntry(body, System.currentTimeMillis() + CACHE_TTL_MS)
                }
                return@withContext body
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries) {
                    delay(400L * (attempt + 1))
                }
            } finally {
                conn.disconnect()
            }
        }
        throw lastError ?: IllegalStateException("Не удалось связаться с API Chess.com. Проверьте подключение к интернету.")
    }

    /**
     * Fetches player profile from Chess.com PubAPI (`/pub/player/{username}`).
     */
    suspend fun fetchChessComProfile(username: String): Result<ChessComProfile> = runCatching {
        val clean = username.trim().lowercase()
        require(clean.isNotEmpty()) { "Введите имя пользователя Chess.com" }
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val raw = fetchJsonWithRetry(
            urlStr = "$CHESS_COM_BASE/player/$encoded",
            notFoundMessage = "Игрок «${username.trim()}» не найден на Chess.com."
        )
        val json = JSONObject(raw)
        ChessComProfile(
            username = json.optString("username", clean),
            avatar = json.optString("avatar", "").takeIf { it.isNotBlank() },
            title = json.optString("title", "").takeIf { it.isNotBlank() },
            country = json.optString("country", "").takeIf { it.isNotBlank() },
            followers = if (json.has("followers")) json.optInt("followers") else null,
            status = json.optString("status", "").takeIf { it.isNotBlank() },
            url = json.optString("url", "").takeIf { it.isNotBlank() }
        )
    }

    /**
     * Fetches list of monthly archive URLs for a player, sorted newest first.
     */
    suspend fun fetchChessComArchives(username: String): Result<List<String>> = runCatching {
        val clean = username.trim().lowercase()
        require(clean.isNotEmpty()) { "Введите имя пользователя Chess.com" }
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val raw = fetchJsonWithRetry(
            urlStr = "$CHESS_COM_BASE/player/$encoded/games/archives",
            notFoundMessage = "Архивы партий игрока «${username.trim()}» не найдены."
        )
        val json = JSONObject(raw)
        val arr = json.optJSONArray("archives") ?: return@runCatching emptyList()
        val list = ArrayList<String>(arr.length())
        for (i in (arr.length() - 1) downTo 0) {
            val url = arr.optString(i, "")
            if (url.isNotBlank()) list.add(url)
        }
        list
    }

    /**
     * Fetches all standard chess games with PGN from a specific Chess.com monthly archive URL.
     */
    suspend fun fetchChessComGamesByArchiveUrl(archiveUrl: String): Result<List<ChessComGameItem>> = runCatching {
        require(archiveUrl.startsWith("https://api.chess.com/pub/player/")) {
            "Некорректный URL архива Chess.com"
        }
        val raw = fetchJsonWithRetry(
            urlStr = archiveUrl,
            notFoundMessage = "Архив за выбранный месяц не найден."
        )
        parseChessComGamesJson(raw)
    }

    /**
     * Fetches games for a specific year and month (`YYYY/MM`).
     */
    suspend fun fetchChessComGamesByMonth(
        username: String,
        year: Int,
        month: Int
    ): Result<List<ChessComGameItem>> = runCatching {
        val clean = username.trim().lowercase()
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val mm = month.coerceIn(1, 12).toString().padStart(2, '0')
        val url = "$CHESS_COM_BASE/player/$encoded/games/$year/$mm"
        val raw = fetchJsonWithRetry(
            urlStr = url,
            notFoundMessage = "За $mm.$year партии не найдены."
        )
        parseChessComGamesJson(raw)
    }

    private fun parseChessComGamesJson(rawJson: String): List<ChessComGameItem> {
        val root = JSONObject(rawJson)
        val arr = root.optJSONArray("games") ?: return emptyList()
        val items = ArrayList<ChessComGameItem>(arr.length())

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val rules = obj.optString("rules", "chess")
            val pgn = obj.optString("pgn", "")
            if (rules != "chess" || pgn.trim().isEmpty()) continue

            val whiteObj = obj.optJSONObject("white")
            val blackObj = obj.optJSONObject("black")

            items.add(
                ChessComGameItem(
                    url = obj.optString("url", ""),
                    pgn = pgn,
                    timeControl = obj.optString("time_control", "-"),
                    endTime = obj.optLong("end_time", 0L),
                    rated = obj.optBoolean("rated", true),
                    timeClass = obj.optString("time_class", "blitz"),
                    rules = rules,
                    white = ChessComPlayerInfo(
                        username = whiteObj?.optString("username", "White") ?: "White",
                        rating = whiteObj?.optInt("rating", 1500) ?: 1500,
                        result = whiteObj?.optString("result", "") ?: ""
                    ),
                    black = ChessComPlayerInfo(
                        username = blackObj?.optString("username", "Black") ?: "Black",
                        rating = blackObj?.optInt("rating", 1500) ?: 1500,
                        result = blackObj?.optString("result", "") ?: ""
                    ),
                    uuid = obj.optString("uuid", "").takeIf { it.isNotBlank() }
                )
            )
        }

        return items.sortedByDescending { it.endTime }
    }

    /**
     * Generates a stable unique key for deduplicating Chess.com games across archives.
     * Matches Web `getChessComGameKey`.
     */
    fun getChessComGameKey(g: ChessComGameItem): String {
        if (!g.uuid.isNullOrBlank()) return g.uuid
        if (g.url.isNotBlank()) return g.url
        return "${g.endTime}_${g.white.username}_${g.black.username}_${g.timeControl}"
    }

    /**
     * Merges and deduplicates Chess.com games, sorting newest first (`endTime` descending).
     * Matches Web `mergeAndDeduplicateChessComGames`.
     */
    fun mergeAndDeduplicateChessComGames(
        existing: List<ChessComGameItem>,
        incoming: List<ChessComGameItem>
    ): List<ChessComGameItem> {
        val map = LinkedHashMap<String, ChessComGameItem>()
        for (g in existing) {
            map[getChessComGameKey(g)] = g
        }
        for (g in incoming) {
            val key = getChessComGameKey(g)
            if (!map.containsKey(key)) {
                map[key] = g
            }
        }
        return map.values.sortedByDescending { it.endTime }
    }

    /**
     * Maps Chess.com player result code into localized label and outcome category.
     * Matches Web `mapChessComResult` in `src/services/ChessComApi/chessComService.ts` 1:1.
     */
    fun mapChessComResult(result: String): ChessComOutcomeInfo {
        return when (result.lowercase()) {
            "win" -> ChessComOutcomeInfo("Победа", "win")
            "agreed", "repetition", "stalemate", "insufficient", "50move", "timevsinsufficient" ->
                ChessComOutcomeInfo("Ничья", "draw")
            "checkmated", "timeout", "resigned", "abandoned", "lose" ->
                ChessComOutcomeInfo("Поражение", "loss")
            else -> ChessComOutcomeInfo(result, "loss")
        }
    }

    /**
     * Formats Unix timestamp (`endTime`) into localized Russian date `dd.MM.yyyy HH:mm`.
     */
    fun formatEndTime(endTimeSeconds: Long): String {
        if (endTimeSeconds <= 0L) return ""
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(endTimeSeconds * 1000L))
    }

    /**
     * Extracts `"YYYY / MM"` label from a Chess.com archive URL.
     */
    fun formatArchiveMonthLabel(archiveUrl: String): String {
        val parts = archiveUrl.trimEnd('/').split("/")
        if (parts.size >= 2) {
            val year = parts[parts.size - 2]
            val month = parts[parts.size - 1]
            return "$year / $month"
        }
        return archiveUrl
    }

    /**
     * Convenience method for batch PGN import from Chess.com.
     */
    suspend fun fetchChessComGames(username: String, maxGames: Int = 25): Result<List<String>> = runCatching {
        val archives = fetchChessComArchives(username).getOrThrow()
        if (archives.isEmpty()) return@runCatching emptyList()

        var collected = emptyList<ChessComGameItem>()
        for (archiveUrl in archives.take(6)) {
            val monthGames = fetchChessComGamesByArchiveUrl(archiveUrl).getOrDefault(emptyList())
            collected = mergeAndDeduplicateChessComGames(collected, monthGames)
            if (collected.size >= maxGames) break
        }
        collected.take(maxGames).map { it.pgn }
    }

    /**
     * Fetches recent games in PGN format from Lichess public API.
     */
    suspend fun fetchLichessGames(username: String, maxGames: Int = 15): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanUser = username.trim()
                require(cleanUser.isNotEmpty()) { "Введите имя пользователя Lichess" }

                val url = URL("https://lichess.org/api/games/user/$cleanUser?max=$maxGames&pgnInJson=false&clocks=false&evals=false&opening=true")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 12000
                    setRequestProperty("Accept", "application/x-chess-pgn")
                    setRequestProperty("User-Agent", "ChessInsightAnalyzer/3.0 (Android Jetpack Compose)")
                }

                try {
                    val code = conn.responseCode
                    if (code != 200) {
                        throw IllegalStateException("Пользователь Lichess «$cleanUser» не найден (HTTP $code)")
                    }
                    val fullText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    splitMultiPgn(fullText).take(maxGames)
                } finally {
                    conn.disconnect()
                }
            }
        }

    fun splitMultiPgn(rawText: String): List<String> {
        val normalized = rawText.replace("\r\n", "\n").trim()
        if (normalized.isEmpty()) return emptyList()

        val games = mutableListOf<String>()
        val lines = normalized.lines()
        val current = StringBuilder()
        var seenMovetext = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[Event ") && seenMovetext && current.isNotEmpty()) {
                games.add(current.toString().trim())
                current.clear()
                seenMovetext = false
            }
            if (trimmed.isNotEmpty() && !trimmed.startsWith("[")) {
                seenMovetext = true
            }
            current.appendLine(line)
        }
        if (current.isNotEmpty()) {
            games.add(current.toString().trim())
        }
        return games.filter { it.isNotBlank() }
    }
}
