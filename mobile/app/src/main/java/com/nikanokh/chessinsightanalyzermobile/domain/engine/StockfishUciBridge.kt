package com.nikanokh.chessinsightanalyzermobile.domain.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessBoardState
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessEngine
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineConfig
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineEvaluation
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineLine
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 1:1 port of Web `src/core/engine/StockfishClient.ts` for Android.
 * Runs the identical Stockfish UCI WASM engine (`assets/stockfish/stockfish.js` + `stockfish.wasm`)
 * inside a background Android WebView with `WebViewAssetLoader`, supporting full UCI commands:
 * `uci`, `isready`, `setoption name MultiPV/Hash/Threads/Ponder`, `position fen`,
 * `go depth <d>` / `go movetime <ms>`, `stop`, and White-POV centipawn/mate normalization.
 */
class StockfishUciBridge(private val appContext: Context) {

    data class StockfishAnalysisResult(
        val bestMoveUci: String,
        val evaluation: EngineEvaluation,
        val lines: List<EngineLine>
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val evalMutex = Mutex()

    private var webView: WebView? = null
    private var isReady = false
    private var initDeferred: CompletableDeferred<Boolean>? = null
    private var readyDeferred: CompletableDeferred<Boolean>? = null

    private var currentConfig = EngineConfig()
    private var currentTurn: PieceColor = PieceColor.WHITE
    private var currentFen: String = ChessEngine.INITIAL_FEN
    private val currentLines = LinkedHashMap<Int, EngineEvaluation>()
    private var activeEvalDeferred: CompletableDeferred<StockfishAnalysisResult>? = null

    private val bridgeHtml = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"></head>
        <body>
        <script>
            var worker = null;
            function bootStockfish() {
                try {
                    worker = new Worker('https://appassets.androidplatform.net/assets/stockfish/stockfish.js');
                    worker.onmessage = function(e) {
                        var line = (typeof e.data === 'string') ? e.data : String(e.data);
                        if (window.AndroidStockfish) {
                            window.AndroidStockfish.onUciLine(line);
                        }
                    };
                    worker.onerror = function(err) {
                        if (window.AndroidStockfish) {
                            window.AndroidStockfish.onEngineError(String(err.message || err));
                        }
                    };
                    worker.postMessage('uci');
                } catch (ex) {
                    if (window.AndroidStockfish) {
                        window.AndroidStockfish.onEngineError(String(ex));
                    }
                }
            }
            function sendUci(cmd) {
                if (worker) {
                    worker.postMessage(cmd);
                }
            }
            bootStockfish();
        </script>
        </body>
        </html>
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun ensureInitialized(): Boolean {
        if (isReady && webView != null) return true
        val deferred = CompletableDeferred<Boolean>()
        initDeferred = deferred

        withContext(Dispatchers.Main) {
            try {
                if (webView == null) {
                    val assetLoader = WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
                        .build()

                    val wv = WebView(appContext)
                    wv.settings.javaScriptEnabled = true
                    wv.settings.allowFileAccess = true
                    wv.settings.domStorageEnabled = true
                    wv.addJavascriptInterface(JsCallback(), "AndroidStockfish")
                    wv.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            return assetLoader.shouldInterceptRequest(request.url)
                        }
                    }
                    wv.loadDataWithBaseURL(
                        "https://appassets.androidplatform.net/assets/stockfish/index.html",
                        bridgeHtml,
                        "text/html",
                        "utf-8",
                        null
                    )
                    webView = wv
                }
            } catch (e: Throwable) {
                deferred.complete(false)
            }
        }

        return withTimeoutOrNull(6500L) { deferred.await() } ?: false
    }

    private fun postUciCommand(cmd: String) {
        val escaped = cmd.replace("\\", "\\\\").replace("'", "\\'")
        mainHandler.post {
            webView?.evaluateJavascript("sendUci('$escaped');", null)
        }
    }

    private suspend fun waitReady(timeoutMs: Long = 2500L): Boolean {
        val def = CompletableDeferred<Boolean>()
        readyDeferred = def
        postUciCommand("isready")
        return withTimeoutOrNull(timeoutMs) { def.await() } ?: false
    }

    suspend fun applyConfig(config: EngineConfig) {
        currentConfig = config
        if (!ensureInitialized()) return
        postUciCommand("setoption name MultiPV value ${config.multiPv.coerceIn(1, 5)}")
        postUciCommand("setoption name Threads value ${config.threads.coerceIn(1, 4)}")
        postUciCommand("setoption name Hash value ${config.hashMb.coerceIn(16, 512)}")
        postUciCommand("setoption name Skill Level value ${config.skillLevel.coerceIn(0, 20)}")
        postUciCommand("setoption name Ponder value false")
        waitReady()
    }

    suspend fun evaluatePosition(
        fen: String,
        config: EngineConfig = currentConfig,
        onUpdate: ((EngineEvaluation, List<EngineLine>) -> Unit)? = null
    ): StockfishAnalysisResult? = evalMutex.withLock {
        if (!ensureInitialized()) return null

        if (config != currentConfig) {
            applyConfig(config)
        }

        val board = try {
            ChessBoardState.fromFen(fen)
        } catch (_: Exception) {
            return null
        }

        val legalMoves = ChessEngine.generateLegalMoves(board)
        if (legalMoves.isEmpty()) {
            val inCheck = ChessEngine.isKingInCheck(board, board.sideToMove)
            val terminalEval = EngineEvaluation(
                cp = if (inCheck) null else 0,
                mate = if (inCheck) {
                    if (board.sideToMove == PieceColor.WHITE) -1 else 1
                } else null,
                depth = 0,
                pv = emptyList(),
                multipv = 1
            )
            return StockfishAnalysisResult(
                bestMoveUci = "",
                evaluation = terminalEval,
                lines = emptyList()
            )
        }

        // Cancel any previous search
        if (activeEvalDeferred != null) {
            postUciCommand("stop")
            waitReady(1200L)
            activeEvalDeferred = null
        }

        currentFen = fen
        currentTurn = board.sideToMove
        currentLines.clear()

        val resultDeferred = CompletableDeferred<StockfishAnalysisResult>()
        activeEvalDeferred = resultDeferred
        this.progressListener = onUpdate

        postUciCommand("position fen $fen")
        if (config.movetimeMs != null && config.movetimeMs > 0) {
            postUciCommand("go movetime ${config.movetimeMs}")
        } else {
            postUciCommand("go depth ${config.depth.coerceIn(1, 30)}")
        }

        val timeoutMs = ((config.movetimeMs ?: (config.depth * 900)).coerceIn(3000, 25000)).toLong()
        val res = withTimeoutOrNull(timeoutMs) {
            resultDeferred.await()
        }

        if (res == null) {
            postUciCommand("stop")
        }
        activeEvalDeferred = null
        progressListener = null
        return res
    }

    fun stop() {
        postUciCommand("stop")
        activeEvalDeferred?.cancel()
        activeEvalDeferred = null
    }

    private var progressListener: ((EngineEvaluation, List<EngineLine>) -> Unit)? = null

    private inner class JsCallback {
        @JavascriptInterface
        fun onUciLine(rawLine: String) {
            val line = rawLine.trim()
            if (line.isEmpty()) return

            if (line == "uciok") {
                postUciCommand("setoption name MultiPV value ${currentConfig.multiPv}")
                postUciCommand("setoption name Hash value ${currentConfig.hashMb}")
                postUciCommand("setoption name Ponder value false")
                postUciCommand("isready")
                return
            }

            if (line == "readyok") {
                isReady = true
                initDeferred?.complete(true)
                initDeferred = null
                readyDeferred?.complete(true)
                readyDeferred = null
                return
            }

            if (line.startsWith("info ") && line.contains(" score ") && line.contains(" pv ")) {
                parseInfoLine(line)
                return
            }

            if (line.startsWith("bestmove ")) {
                val parts = line.split(Regex("\\s+"))
                val bestMoveUci = parts.getOrNull(1)?.takeIf { it != "(none)" } ?: ""
                val def = activeEvalDeferred
                if (def != null && !def.isCompleted) {
                    val primaryEval = currentLines[1] ?: EngineEvaluation(
                        cp = 0,
                        mate = null,
                        depth = currentConfig.depth,
                        pv = if (bestMoveUci.isNotEmpty()) listOf(bestMoveUci) else emptyList(),
                        multipv = 1
                    )
                    val structuredLines = buildEngineLines()
                    val finalBestMove = bestMoveUci.ifEmpty {
                        primaryEval.pv.firstOrNull() ?: ""
                    }
                    def.complete(
                        StockfishAnalysisResult(
                            bestMoveUci = finalBestMove,
                            evaluation = primaryEval,
                            lines = structuredLines
                        )
                    )
                }
            }
        }

        @JavascriptInterface
        fun onEngineError(errorMsg: String) {
            initDeferred?.complete(false)
            activeEvalDeferred?.cancel()
        }
    }

    private fun parseInfoLine(line: String) {
        val depthMatch = Regex("\\bdepth\\s+(\\d+)").find(line) ?: return
        val depth = depthMatch.groupValues[1].toIntOrNull() ?: return
        val seldepth = Regex("\\bseldepth\\s+(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
        val multipv = Regex("\\bmultipv\\s+(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val nodes = Regex("\\bnodes\\s+(\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull()
        val nps = Regex("\\bnps\\s+(\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull()
        val timeMs = Regex("\\btime\\s+(\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

        val turnMultiplier = if (currentTurn == PieceColor.WHITE) 1 else -1

        var cp: Int? = null
        var mate: Int? = null

        val cpMatch = Regex("\\bscore\\s+cp\\s+(-?\\d+)").find(line)
        val mateMatch = Regex("\\bscore\\s+mate\\s+(-?\\d+)").find(line)

        if (cpMatch != null) {
            cp = (cpMatch.groupValues[1].toIntOrNull() ?: 0) * turnMultiplier
        } else if (mateMatch != null) {
            val rawMate = mateMatch.groupValues[1].toIntOrNull() ?: 0
            mate = rawMate * turnMultiplier
        }

        val pvIndex = line.indexOf(" pv ")
        val pv = if (pvIndex != -1) {
            line.substring(pvIndex + 4).trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        val evalObj = EngineEvaluation(
            cp = cp,
            mate = mate,
            depth = depth,
            seldepth = seldepth,
            nodes = nodes,
            nps = nps,
            timeMs = timeMs,
            pv = pv,
            multipv = multipv
        )
        currentLines[multipv] = evalObj

        val primary = currentLines[1]
        if (primary != null && progressListener != null) {
            progressListener?.invoke(primary, buildEngineLines())
        }
    }

    private fun buildEngineLines(): List<EngineLine> {
        val sorted = currentLines.values.sortedBy { it.multipv }
        return sorted.map { ev ->
            val pvSan = ArrayList<String>()
            var bestMoveSan = ev.pv.firstOrNull() ?: ""
            try {
                var board = ChessBoardState.fromFen(currentFen)
                for ((idx, uci) in ev.pv.take(8).withIndex()) {
                    val m = ChessEngine.parseUciMove(board, uci) ?: break
                    val san = ChessEngine.moveToSan(board, m)
                    if (idx == 0) bestMoveSan = san
                    pvSan.add(san)
                    board = ChessEngine.applyMove(board, m)
                }
            } catch (_: Exception) {
                // ignore
            }
            EngineLine(
                multipv = ev.multipv,
                depth = ev.depth,
                cp = ev.cp,
                mate = ev.mate,
                pvUci = ev.pv,
                pvSan = pvSan,
                bestMoveUci = ev.pv.firstOrNull() ?: "",
                bestMoveSan = bestMoveSan
            )
        }
    }
}
