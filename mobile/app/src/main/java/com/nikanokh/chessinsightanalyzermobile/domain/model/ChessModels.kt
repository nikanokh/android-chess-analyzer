package com.nikanokh.chessinsightanalyzermobile.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.nikanokh.chessinsightanalyzermobile.R

enum class PieceColor(val code: Char) {
    WHITE('w'),
    BLACK('b');

    val opposite: PieceColor
        get() = if (this == WHITE) BLACK else WHITE

    companion object {
        fun fromCode(code: Char): PieceColor = if (code == 'b') BLACK else WHITE
    }
}

enum class PieceType(val code: Char, val sanLetter: String, val centipawnValue: Int) {
    PAWN('p', "", 100),
    KNIGHT('n', "N", 320),
    BISHOP('b', "B", 330),
    ROOK('r', "R", 500),
    QUEEN('q', "Q", 900),
    KING('k', "K", 20000);

    companion object {
        fun fromChar(c: Char): PieceType? = entries.firstOrNull { it.code == c.lowercaseChar() }
    }
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.code.uppercaseChar() else type.code

    val assetKey: String
        get() = "${color.code}${type.code}"

    @get:DrawableRes
    val drawableRes: Int
        get() = when (assetKey) {
            "wp" -> R.drawable.piece_wp
            "wn" -> R.drawable.piece_wn
            "wb" -> R.drawable.piece_wb
            "wr" -> R.drawable.piece_wr
            "wq" -> R.drawable.piece_wq
            "wk" -> R.drawable.piece_wk
            "bp" -> R.drawable.piece_bp
            "bn" -> R.drawable.piece_bn
            "bb" -> R.drawable.piece_bb
            "br" -> R.drawable.piece_br
            "bq" -> R.drawable.piece_bq
            "bk" -> R.drawable.piece_bk
            else -> R.drawable.piece_wp
        }
}

/**
 * Exact 11 Move Categories matching Web `src/types/chess.ts` and `src/utils/assets.ts`
 */
enum class MoveCategory(
    val id: String,
    val labelRu: String,
    val symbol: String,
    val colorHex: Long,
    @DrawableRes val iconRes: Int
) {
    BRILLIANT("Brilliant", "Бриллиантовый ход", "!!", 0xFF1BACA6, R.drawable.cat_brilliant),
    GREAT("Great", "Отличный ход", "!", 0xFF5C8BB0, R.drawable.cat_great),
    BEST("Best", "Лучший ход", "★", 0xFF779954, R.drawable.cat_best),
    EXCELLENT("Excellent", "Отличный ход", "👍", 0xFF81B64C, R.drawable.cat_excellent),
    GOOD("Good", "Хороший ход", "✓", 0xFF95B776, R.drawable.cat_good),
    BOOK("Book", "Теоретический ход", "📖", 0xFFA88865, R.drawable.cat_book),
    FORCED("Forced", "Форсированный ход", "→", 0xFF779954, R.drawable.cat_forced),
    INACCURACY("Inaccuracy", "Неточность", "?!", 0xFFF0C15C, R.drawable.cat_inaccuracy),
    MISTAKE("Mistake", "Ошибка", "?", 0xFFE6912C, R.drawable.cat_mistake),
    MISSED_CHANCE("MissedChance", "Упущенная возможность", "✖", 0xFFEE6B5E, R.drawable.cat_missed_chance),
    BLUNDER("Blunder", "Зевок", "??", 0xFFCA3431, R.drawable.cat_blunder);

    val color: Color
        get() = Color(colorHex)

    companion object {
        fun fromId(id: String): MoveCategory =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
                ?: GOOD
    }
}

data class EngineConfig(
    val depth: Int = 18,
    val multiPv: Int = 3,
    val threads: Int = 2,
    val hashMb: Int = 64,
    val movetimeMs: Int? = null,
    val skillLevel: Int = 20
)

data class ClassificationThresholds(
    val bestMaxCpl: Int = 20,
    val excellentMaxCpl: Int = 50,
    val goodMaxCpl: Int = 95,
    val inaccuracyMaxCpl: Int = 175,
    val mistakeMaxCpl: Int = 300
)

data class EngineEvaluation(
    val cp: Int? = 0,          // Centipawn score normalized to White's perspective
    val mate: Int? = null,     // Moves to mate normalized to White's perspective (+White mates, -Black mates)
    val depth: Int = 18,
    val seldepth: Int? = null,
    val nodes: Long? = null,
    val nps: Long? = null,
    val timeMs: Long = 0L,
    val pv: List<String> = emptyList(),
    val multipv: Int = 1
) {
    /**
     * Normalized centipawns for graphs/legacy helpers (mate converted to +-10000).
     */
    val effectiveCentipawns: Int
        get() = when {
            mate != null -> if (mate > 0) 10000 - mate * 10 else -10000 - mate * 10
            else -> cp ?: 0
        }
}

data class EngineLine(
    val multipv: Int,
    val depth: Int,
    val cp: Int?,
    val mate: Int?,
    val pvUci: List<String>,
    val pvSan: List<String>,
    val bestMoveUci: String,
    val bestMoveSan: String
)

data class MoveAnalysis(
    val id: String,
    val ply: Int,
    val moveNumber: Int,
    val color: PieceColor,
    val san: String,
    val uci: String,
    val from: String,
    val to: String,
    val piece: Char,
    val captured: Char? = null,
    val promotion: Char? = null,
    val fenBefore: String,
    val fenAfter: String,
    val isBook: Boolean = false,
    val openingEco: String? = null,
    val openingName: String? = null,
    val legalMovesCount: Int = 20,
    val evalBefore: EngineEvaluation? = null,
    val evalAfter: EngineEvaluation? = null,
    val bestMoveUci: String? = null,
    val bestMoveSan: String? = null,
    val topLines: List<EngineLine> = emptyList(),
    val cpLoss: Int = 0,
    val winProbBefore: Double = 50.0,
    val winProbAfter: Double = 50.0,
    val winProbLoss: Double = 0.0,
    val accuracy: Double = 100.0,
    val category: MoveCategory = MoveCategory.GOOD,
    val isSacrifice: Boolean = false,
    val isOnlyBestMove: Boolean = false,
    val isMissedTactic: Boolean = false,
    val explanation: String = "",
    val comment: String = ""
) {
    val centipawnsAfter: Int
        get() = evalAfter?.effectiveCentipawns ?: 0

    val centipawnLoss: Int
        get() = cpLoss
}

data class MoveTreeNode(
    val id: String,
    val parentId: String?,
    val ply: Int,
    val moveNumber: Int,
    val color: PieceColor,
    val san: String,
    val uci: String,
    val from: String,
    val to: String,
    val fenBefore: String,
    val fenAfter: String,
    val childrenIds: List<String> = emptyList(),
    val analysis: MoveAnalysis? = null,
    val comment: String = ""
)

data class GameHeaders(
    val event: String = "Rated Blitz Game",
    val site: String = "Chess.com / Lichess",
    val date: String = "2025.05.01",
    val round: String = "-",
    val white: String = "White Player",
    val black: String = "Black Player",
    val result: String = "*",
    val whiteElo: String = "1850",
    val blackElo: String = "1840",
    val timeControl: String = "180+2",
    val eco: String = "",
    val opening: String = ""
)

data class PlayerStatistics(
    val accuracy: Double = 0.0,
    val averageCentipawnLoss: Double = 0.0,
    val estimatedElo: Int = 1500,
    val totalMoves: Int = 0,
    val categoryCounts: Map<MoveCategory, Int> = MoveCategory.entries.associateWith { 0 }
)

data class GameStatistics(
    val white: PlayerStatistics = PlayerStatistics(),
    val black: PlayerStatistics = PlayerStatistics(),
    val openingAccuracyWhite: Double = 0.0,
    val middlegameAccuracyWhite: Double = 0.0,
    val endgameAccuracyWhite: Double = 0.0,
    val openingAccuracyBlack: Double = 0.0,
    val middlegameAccuracyBlack: Double = 0.0,
    val endgameAccuracyBlack: Double = 0.0
)

data class OpeningDeviationInfo(
    val ply: Int,
    val moveNumber: Int,
    val color: PieceColor,
    val playedSan: String,
    val playedUci: String,
    val bookMovesSan: List<String>
)

data class OpeningAnalysisSummary(
    val eco: String = "A00",
    val name: String = "Нестандартное начало",
    val variation: String? = null,
    val lastBookPly: Int = 0,
    val deviation: OpeningDeviationInfo? = null
)

data class SavedGameRecord(
    val id: String,
    val title: String,
    val white: String,
    val black: String,
    val whiteElo: String,
    val blackElo: String,
    val result: String,
    val event: String,
    val date: String,
    val eco: String,
    val openingName: String,
    val pgn: String,
    val initialFen: String,
    val movesCount: Int,
    val whiteAccuracy: Double?,
    val blackAccuracy: Double?,
    val isFavorite: Boolean,
    val folderTag: String,
    val createdAt: Long,
    val analysesJson: String? = null
)

data class RepertoireEntry(
    val id: String,
    val title: String,
    val color: PieceColor,
    val eco: String,
    val pgnMoves: String,
    val targetFen: String,
    val notes: String,
    val tags: String,
    val practicedCount: Int,
    val correctCount: Int,
    val lastPracticedAt: Long?
)

data class UserArrow(
    val from: String,
    val to: String,
    val colorHex: String
)

data class SquareHighlight(
    val square: String,
    val colorHex: String
)

data class BoardThemePreset(
    val id: String,
    val nameRu: String,
    val lightSquareHex: Long,
    val darkSquareHex: Long
) {
    val lightSquare: Color get() = Color(lightSquareHex)
    val darkSquare: Color get() = Color(darkSquareHex)
}

val BOARD_THEME_PRESETS = listOf(
    BoardThemePreset("classic_green", "Классический зелёный", 0xFFEEEED2, 0xFF769656),
    BoardThemePreset("tournament_wood", "Турнирное дерево", 0xFFF0D9B5, 0xFFB58863),
    BoardThemePreset("sea_ice", "Морской лёд", 0xFFDEE3E6, 0xFF8CA2AD),
    BoardThemePreset("graphite_minimal", "Графитовый минимализм", 0xFFE5E7EB, 0xFF4B5563)
)

/**
 * Full synchronized settings matching Web `src/types/chess.ts` and `src/stores/useSettingsStore.ts`
 */
data class AppSettings(
    val boardThemeId: String = "classic_green",
    val lightSquareColorHex: Long = 0xFFEEEED2,
    val darkSquareColorHex: Long = 0xFF769656,
    val pieceSet: String = "neo",              // "neo" (original PNGs) | "classic_svg" (vector SVG)
    val boardSize: String = "auto",            // "auto" | "small" | "medium" | "large"
    val enableAnimations: Boolean = true,
    val highlightLastMove: Boolean = true,
    val highlightLegalMoves: Boolean = true,
    val showCoordinates: Boolean = true,
    val showEvalBar: Boolean = true,
    val showLeftSidebar: Boolean = true,
    val showUserArrows: Boolean = true,
    val showEngineArrows: Boolean = true,
    val userArrowColorHex: String = "#F59E0B",
    val engineArrowColorHex: String = "#10B981",
    val secondaryEngineArrowColorHex: String = "#3B82F6",
    val arrowWidth: Int = 20,                  // 14..32
    val arrowOpacity: Float = 0.85f,           // 0.40..1.00
    val engineConfig: EngineConfig = EngineConfig(
        depth = 18,
        multiPv = 3,
        threads = 2,
        hashMb = 64,
        movetimeMs = null,
        skillLevel = 20
    ),
    val thresholds: ClassificationThresholds = ClassificationThresholds(
        bestMaxCpl = 20,
        excellentMaxCpl = 50,
        goodMaxCpl = 95,
        inaccuracyMaxCpl = 175,
        mistakeMaxCpl = 300
    )
) {
    // Convenience aliases for existing callers
    val engineDepth: Int get() = engineConfig.depth
    val multiPv: Int get() = engineConfig.multiPv
    val engineThreads: Int get() = engineConfig.threads
    val engineHashMb: Int get() = engineConfig.hashMb
    val showBestMoveArrow: Boolean get() = showEngineArrows
    val autoAnalyzeOnLoad: Boolean get() = true
}
