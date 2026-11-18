package com.nikanokh.chessinsightanalyzermobile.domain.model

import androidx.compose.ui.graphics.Color

enum class PieceColor(val code: Char) {
    WHITE('w'),
    BLACK('b');

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(
    val fenChar: Char,
    val sanChar: String,
    val materialValue: Int,
    val whiteUnicode: String,
    val blackUnicode: String
) {
    PAWN('p', "", 1, "♙", "♟"),
    KNIGHT('n', "N", 3, "♘", "♞"),
    BISHOP('b', "B", 3, "♗", "♝"),
    ROOK('r', "R", 5, "♖", "♜"),
    QUEEN('q', "Q", 9, "♕", "♛"),
    KING('k', "K", 0, "♔", "♚");

    fun unicodeFor(color: PieceColor): String =
        if (color == PieceColor.WHITE) whiteUnicode else blackUnicode
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    val unicode: String get() = type.unicodeFor(color)
}

data class Square(
    val file: Int, // 0..7 ('a'..'h')
    val rank: Int  // 0..7 ('1'..'8')
) {
    val algebraic: String
        get() = "${('a' + file)}${('1' + rank)}"

    val isLightSquare: Boolean
        get() = (file + rank) % 2 != 0

    companion object {
        fun fromAlgebraic(str: String): Square? {
            if (str.length != 2) return null
            val f = str[0].lowercaseChar() - 'a'
            val r = str[1] - '1'
            if (f !in 0..7 || r !in 0..7) return null
            return Square(f, r)
        }
    }
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val captured: Piece? = null,
    val promotion: PieceType? = null,
    val isKingsideCastle: Boolean = false,
    val isQueensideCastle: Boolean = false,
    val isEnPassant: Boolean = false
) {
    val uci: String
        get() = "${from.algebraic}${to.algebraic}${promotion?.fenChar ?: ""}"
}

enum class MoveCategory(
    val labelRu: String,
    val badgeSymbol: String,
    val colorHex: Long
) {
    BRILLIANT("Бриллиантовый", "!!", 0xFF1BACA6),
    GREAT("Отличный", "!", 0xFF5C8BB0),
    BEST("Лучший", "★", 0xFF96BC4B),
    BOOK("Теория", "📖", 0xFFA88865),
    GOOD("Хороший", "✓", 0xFF81B64C),
    INACCURACY("Неточность", "?!", 0xFFF7C631),
    MISTAKE("Ошибка", "?", 0xFFE68A00),
    BLUNDER("Зевок", "??", 0xFFCA3431);

    val composeColor: Color get() = Color(colorHex)
}

data class MoveAnalysis(
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val mateAfter: Int? = null,
    val cpl: Int,
    val bestMoveUci: String,
    val bestMoveSan: String,
    val category: MoveCategory,
    val pvLine: String = ""
)

data class MoveNode(
    val id: String,
    val ply: Int,
    val moveNumber: Int,
    val color: PieceColor,
    val san: String,
    val uci: String,
    val fromSquare: String,
    val toSquare: String,
    val fenBefore: String,
    val fenAfter: String,
    val analysis: MoveAnalysis? = null
)

data class GameMetadata(
    val whiteName: String = "Белые",
    val blackName: String = "Чёрные",
    val whiteRating: String = "",
    val blackRating: String = "",
    val result: String = "*",
    val date: String = "2025.02.25",
    val event: String = "Классическая партия",
    val site: String = "Local",
    val eco: String = "A00",
    val platform: String = "PGN"
) {
    val fullTitle: String
        get() = "$whiteName vs $blackName ($result)"
}

data class OpeningInfo(
    val eco: String,
    val name: String,
    val variation: String = "",
    val movesPrefix: String = ""
) {
    val fullDisplayName: String
        get() = if (variation.isNotBlank() &&
            variation != "Основная линия" &&
            variation != "Начальная позиция"
        ) {
            "$name: $variation"
        } else {
            name
        }
}

data class GameStatistics(
    val whiteAccuracy: Double,
    val blackAccuracy: Double,
    val whiteAvgCpl: Int,
    val blackAvgCpl: Int,
    val whiteCategoryCounts: Map<MoveCategory, Int>,
    val blackCategoryCounts: Map<MoveCategory, Int>
)

data class CapturedSummary(
    val capturedByWhite: List<PieceType>,
    val capturedByBlack: List<PieceType>,
    val materialDiffWhitePov: Int
)

data class SavedGameRecord(
    val id: String,
    val dedupKey: String,
    val createdAt: Long,
    val metadata: GameMetadata,
    val opening: OpeningInfo,
    val pgn: String,
    val finalFen: String,
    val statistics: GameStatistics? = null,
    val moves: List<MoveNode> = emptyList()
)

data class VariationStat(
    val variationName: String,
    val gamesCount: Int,
    val winRate: Int,
    val drawRate: Int,
    val lossRate: Int
)

data class RepertoireOpeningStat(
    val key: String,
    val eco: String,
    val openingName: String,
    val gamesCount: Int,
    val winRate: Int,
    val drawRate: Int,
    val lossRate: Int,
    val variations: List<VariationStat>
)

data class AppSettings(
    val boardOrientationWhite: Boolean = true,
    val showEvalBar: Boolean = true,
    val highlightLegalMoves: Boolean = true,
    val showEngineArrows: Boolean = true,
    val autoScrollMoves: Boolean = true,
    val engineDepth: Int = 14,
    val lightSquareHex: Long = 0xFFEEEED2,
    val darkSquareHex: Long = 0xFF769656
)
