package com.nikanokh.chessinsightanalyzermobile.domain.chess

import com.nikanokh.chessinsightanalyzermobile.domain.model.OpeningAnalysisSummary
import com.nikanokh.chessinsightanalyzermobile.domain.model.OpeningDeviationInfo
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor

data class OpeningEntry(
    val eco: String,
    val name: String,
    val moves: String
)

/**
 * 1:1 port of Web `src/core/chess/OpeningBook.ts`
 * Includes all 42 ECO entries, legal en-passant normalized FEN keys,
 * `isPositionInTheory`, `isBookMove`, and `analyzeMainlineOpening`.
 */
object EcoOpeningBook {

    val INITIAL_ECO_ENTRIES: List<OpeningEntry> = listOf(
        // King's Pawn (e4)
        OpeningEntry("B00", "Дебют королевской пешки", "1. e4"),
        OpeningEntry("C20", "Открытая партия", "1. e4 e5"),
        OpeningEntry("C40", "Дебют королевского коня", "1. e4 e5 2. Nf3"),
        OpeningEntry("C44", "Дебют слона / Развивающий ход", "1. e4 e5 2. Nf3 Nc6"),
        OpeningEntry("C50", "Итальянская партия", "1. e4 e5 2. Nf3 Nc6 3. Bc4"),
        OpeningEntry("C53", "Итальянская партия: Тихая игра (Giuoco Piano)", "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5"),
        OpeningEntry("C54", "Итальянская партия: Вариант с c3", "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. c3 Nf6 5. d3"),
        OpeningEntry("C55", "Защита двух коней", "1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6"),
        OpeningEntry("C60", "Испанская партия (Руй Лопес)", "1. e4 e5 2. Nf3 Nc6 3. Bb5"),
        OpeningEntry("C65", "Испанская партия: Берлинская защита", "1. e4 e5 2. Nf3 Nc6 3. Bb5 Nf6"),
        OpeningEntry("C68", "Испанская партия: Разменный вариант", "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Bxc6"),
        OpeningEntry("C70", "Испанская партия: Защита Морфи", "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4"),
        OpeningEntry("C84", "Испанская партия: Закрытая система", "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7"),
        OpeningEntry("C42", "Русская партия (Защита Петрова)", "1. e4 e5 2. Nf3 Nf6"),
        OpeningEntry("C45", "Шотландская партия", "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Nxd4"),
        OpeningEntry("C25", "Венская партия", "1. e4 e5 2. Nc3"),
        OpeningEntry("C30", "Королевский гамбит", "1. e4 e5 2. f4"),

        // Sicilian Defense
        OpeningEntry("B20", "Сицилианская защита", "1. e4 c5"),
        OpeningEntry("B22", "Сицилианская защита: Вариант Алапина", "1. e4 c5 2. c3"),
        OpeningEntry("B23", "Закрытая сицилианская защита", "1. e4 c5 2. Nc3"),
        OpeningEntry("B30", "Сицилианская защита: Старый вариант", "1. e4 c5 2. Nf3 Nc6"),
        OpeningEntry("B33", "Сицилианская защита: Челябинский вариант (Свешникова)", "1. e4 c5 2. Nf3 Nc6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 e5"),
        OpeningEntry("B40", "Сицилианская защита: Вариант с e6", "1. e4 c5 2. Nf3 e6"),
        OpeningEntry("B50", "Сицилианская защита", "1. e4 c5 2. Nf3 d6"),
        OpeningEntry("B90", "Сицилианская защита: Вариант Найдорфа", "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6"),
        OpeningEntry("B70", "Сицилианская защита: Вариант Дракона", "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 g6"),

        // French, Caro-Kann, Scandinavian, Pirc, Alekhine
        OpeningEntry("C00", "Французская защита", "1. e4 e6"),
        OpeningEntry("C02", "Французская защита: Закрытый вариант", "1. e4 e6 2. d4 d5 3. e5"),
        OpeningEntry("C11", "Французская защита: Классическая система", "1. e4 e6 2. d4 d5 3. Nc3 Nf6"),
        OpeningEntry("B10", "Защита Каро-Канн", "1. e4 c6"),
        OpeningEntry("B12", "Защита Каро-Канн: Закрытый вариант", "1. e4 c6 2. d4 d5 3. e5"),
        OpeningEntry("B18", "Защита Каро-Канн: Классический вариант", "1. e4 c6 2. d4 d5 3. Nc3 dxe4 4. Nxe4 Bf5"),
        OpeningEntry("B01", "Скандинавская защита", "1. e4 d5"),
        OpeningEntry("B07", "Защита Пирца-Уфимцева", "1. e4 d6 2. d4 Nf6 3. Nc3 g6"),
        OpeningEntry("B02", "Защита Алехина", "1. e4 Nf6"),

        // Queen's Pawn (d4)
        OpeningEntry("D00", "Дебют ферзевой пешки", "1. d4 d5"),
        OpeningEntry("D02", "Лондонская система", "1. d4 d5 2. Nf3 Nf6 3. Bf4"),
        OpeningEntry("D06", "Ферзевый гамбит", "1. d4 d5 2. c4"),
        OpeningEntry("D10", "Славянская защита", "1. d4 d5 2. c4 c6"),
        OpeningEntry("D30", "Отказанный ферзевый гамбит", "1. d4 d5 2. c4 e6"),
        OpeningEntry("D20", "Принятый ферзевый гамбит", "1. d4 d5 2. c4 dxc4"),
        OpeningEntry("A45", "Индийская защита", "1. d4 Nf6"),
        OpeningEntry("E60", "Староиндийская защита", "1. d4 Nf6 2. c4 g6"),
        OpeningEntry("E20", "Защита Нимцовича", "1. d4 Nf6 2. c4 e6 3. Nc3 Bb4"),
        OpeningEntry("D80", "Защита Грюнфельда", "1. d4 Nf6 2. c4 g6 3. Nc3 d5"),
        OpeningEntry("A80", "Голландская защита", "1. d4 f5"),

        // Flank Openings
        OpeningEntry("A10", "Английское начало", "1. c4"),
        OpeningEntry("A04", "Дебют Рети", "1. Nf3"),
        OpeningEntry("A00", "Дебют Сокольского (Орангутан)", "1. b4"),
        OpeningEntry("A02", "Дебют Берда", "1. f4")
    )

    private val fenBookMap: Map<String, OpeningEntry>
    private val bookTransitionMap: Map<String, Set<String>>

    init {
        val fenMap = LinkedHashMap<String, OpeningEntry>()
        val transitions = LinkedHashMap<String, MutableSet<String>>()

        val startKey = normalizeFenKey(ChessEngine.INITIAL_FEN)
        fenMap[startKey] = OpeningEntry("A00", "Начальная позиция", "")

        for (entry in INITIAL_ECO_ENTRIES) {
            var board = ChessBoardState.fromFen(ChessEngine.INITIAL_FEN)
            val tokens = entry.moves
                .replace(Regex("\\d+\\.+"), "")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            for (san in tokens) {
                val beforeKey = normalizeLegalEpFenKey(board)
                val move = ChessEngine.parseSanMove(board, san) ?: break
                board = ChessEngine.applyMove(board, move)
                val afterKey = normalizeLegalEpFenKey(board)

                val set = transitions.getOrPut(beforeKey) { LinkedHashSet() }
                set.add(san)
                fenMap[afterKey] = entry
            }
        }

        fenBookMap = fenMap
        bookTransitionMap = transitions
    }

    /**
     * Normalizes FEN to first 4 fields (pieces, turn, castling, en-passant).
     */
    fun normalizeFenKey(fen: String): String {
        return fen.trim().split(Regex("\\s+")).take(4).joinToString(" ")
    }

    /**
     * Normalizes FEN while clearing pseudo en-passant square unless a legal
     * en-passant capture actually exists on the board. Matches Web `normalizeLegalEpFenKey`.
     */
    fun normalizeLegalEpFenKey(fen: String): String {
        return try {
            val board = ChessBoardState.fromFen(fen)
            normalizeLegalEpFenKey(board)
        } catch (_: Exception) {
            normalizeFenKey(fen)
        }
    }

    fun normalizeLegalEpFenKey(board: ChessBoardState): String {
        val rawParts = board.toFen().trim().split(Regex("\\s+")).take(4).toMutableList()
        if (rawParts.size >= 4 && rawParts[3] != "-") {
            val hasLegalEp = ChessEngine.generateLegalMoves(board).any { it.isEnPassant }
            if (!hasLegalEp) {
                rawParts[3] = "-"
            }
        }
        return rawParts.joinToString(" ")
    }

    fun getOpeningByFen(fen: String): OpeningEntry? {
        val key = normalizeLegalEpFenKey(fen)
        return fenBookMap[key] ?: fenBookMap[normalizeFenKey(fen)]
    }

    fun isPositionInTheory(fen: String): Boolean {
        val key = normalizeLegalEpFenKey(fen)
        return fenBookMap.containsKey(key) || fenBookMap.containsKey(normalizeFenKey(fen))
    }

    fun isBookMove(fenBefore: String, fenAfter: String, ply: Int): Boolean {
        if (ply > 24) return false
        return isPositionInTheory(fenBefore) && isPositionInTheory(fenAfter)
    }

    fun getKnownContinuations(fenBefore: String): List<String> {
        val key = normalizeLegalEpFenKey(fenBefore)
        val set = bookTransitionMap[key] ?: bookTransitionMap[normalizeFenKey(fenBefore)]
        return set?.toList() ?: emptyList()
    }

    fun splitOpeningTitle(fullName: String): Pair<String, String?> {
        val idx = fullName.indexOf(':')
        if (idx == -1) return fullName.trim() to null
        return fullName.substring(0, idx).trim() to fullName.substring(idx + 1).trim()
    }

    data class MainlineMoveInput(
        val ply: Int,
        val moveNumber: Int,
        val color: PieceColor,
        val san: String,
        val uci: String,
        val fenBefore: String,
        val fenAfter: String
    )

    fun analyzeMainlineOpening(
        moves: List<MainlineMoveInput>,
        fallbackEco: String? = null,
        fallbackName: String? = null
    ): OpeningAnalysisSummary {
        var matchedEntry = OpeningEntry(
            eco = fallbackEco?.takeIf { it.isNotBlank() } ?: "A00",
            name = fallbackName?.takeIf { it.isNotBlank() } ?: "Нестандартное начало",
            moves = ""
        )
        var lastBookPly = 0
        var deviation: OpeningDeviationInfo? = null
        var inBookChain = true

        for (m in moves) {
            val afterOpening = getOpeningByFen(m.fenAfter)
            if (inBookChain && m.ply <= 24 && isPositionInTheory(m.fenBefore) && afterOpening != null) {
                matchedEntry = afterOpening
                lastBookPly = m.ply
            } else if (inBookChain) {
                inBookChain = false
                val knownBookMoves = getKnownContinuations(m.fenBefore)
                deviation = OpeningDeviationInfo(
                    ply = m.ply,
                    moveNumber = m.moveNumber,
                    color = m.color,
                    playedSan = m.san,
                    playedUci = m.uci,
                    bookMovesSan = knownBookMoves
                )
            }
        }

        val (name, variation) = splitOpeningTitle(matchedEntry.name)
        return OpeningAnalysisSummary(
            eco = matchedEntry.eco,
            name = name,
            variation = variation,
            lastBookPly = lastBookPly,
            deviation = deviation
        )
    }

    /**
     * Backwards-compatible helper for PGN move lists.
     */
    fun detectOpening(sanMoves: List<String>): Pair<String, String> {
        var board = ChessBoardState.fromFen(ChessEngine.INITIAL_FEN)
        var matched = OpeningEntry("A00", "Нестандартное начало", "")
        for ((index, san) in sanMoves.withIndex()) {
            val ply = index + 1
            if (ply > 24) break
            val beforeFen = board.toFen()
            val move = ChessEngine.parseSanMove(board, san) ?: break
            board = ChessEngine.applyMove(board, move)
            val afterFen = board.toFen()
            val entry = getOpeningByFen(afterFen)
            if (isPositionInTheory(beforeFen) && entry != null) {
                matched = entry
            } else {
                break
            }
        }
        return matched.eco to matched.name
    }
}
