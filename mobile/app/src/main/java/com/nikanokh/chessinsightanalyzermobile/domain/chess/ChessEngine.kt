package com.nikanokh.chessinsightanalyzermobile.domain.chess

import com.nikanokh.chessinsightanalyzermobile.domain.model.ChessPiece
import com.nikanokh.chessinsightanalyzermobile.domain.model.GameHeaders
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveTreeNode
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceType
import kotlin.math.abs

data class ChessMove(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val isCastlesKingside: Boolean = false,
    val isCastlesQueenside: Boolean = false,
    val isEnPassant: Boolean = false,
    val capturedPiece: ChessPiece? = null
) {
    val fromAlgebraic: String
        get() = ChessEngine.squareToAlgebraic(from)

    val toAlgebraic: String
        get() = ChessEngine.squareToAlgebraic(to)

    val uci: String
        get() = "$fromAlgebraic$toAlgebraic${promotion?.code ?: ""}"
}

data class ChessBoardState(
    val squares: List<ChessPiece?>, // 64 squares: 0 = a8 .. 7 = h8, ..., 56 = a1 .. 63 = h1
    val sideToMove: PieceColor = PieceColor.WHITE,
    val castlingWhiteKingside: Boolean = true,
    val castlingWhiteQueenside: Boolean = true,
    val castlingBlackKingside: Boolean = true,
    val castlingBlackQueenside: Boolean = true,
    val enPassantSquare: Int? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
) {
    fun pieceAt(square: Int): ChessPiece? = squares.getOrNull(square)

    fun pieceAt(algebraic: String): ChessPiece? {
        val sq = ChessEngine.algebraicToSquare(algebraic)
        return if (sq in 0..63) squares[sq] else null
    }

    fun toFen(): String = ChessEngine.toFen(this)

    companion object {
        fun fromFen(fen: String): ChessBoardState = ChessEngine.parseFen(fen)
    }
}

data class ParsedPgnGame(
    val headers: GameHeaders,
    val initialFen: String,
    val nodes: List<MoveTreeNode>,
    val sanMoves: List<String>
)

/**
 * Complete FIDE-compliant chess rules engine (castling, en-passant, pins, check, checkmate,
 * stalemate, SAN generation/parsing, FEN & PGN parsing).
 */
object ChessEngine {
    const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun squareToRow(sq: Int): Int = sq / 8
    fun squareToCol(sq: Int): Int = sq % 8
    fun rowColToSquare(row: Int, col: Int): Int = row * 8 + col

    fun squareToAlgebraic(sq: Int): String {
        if (sq !in 0..63) return "a1"
        val file = ('a'.code + (sq % 8)).toChar()
        val rank = 8 - (sq / 8)
        return "$file$rank"
    }

    fun algebraicToSquare(alg: String): Int {
        if (alg.length < 2) return -1
        val file = alg[0].lowercaseChar().code - 'a'.code
        val rank = alg[1].digitToIntOrNull() ?: return -1
        val row = 8 - rank
        if (file !in 0..7 || row !in 0..7) return -1
        return row * 8 + file
    }

    fun parseFen(fen: String): ChessBoardState {
        val trimmed = fen.trim().ifEmpty { INITIAL_FEN }
        val parts = trimmed.split(Regex("\\s+"))
        val boardPart = parts.getOrNull(0) ?: INITIAL_FEN.substringBefore(" ")
        val turnPart = parts.getOrNull(1) ?: "w"
        val castlingPart = parts.getOrNull(2) ?: "-"
        val epPart = parts.getOrNull(3) ?: "-"
        val halfMove = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullMove = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val squares = MutableList<ChessPiece?>(64) { null }
        val ranks = boardPart.split("/")
        for (r in 0 until minOf(8, ranks.size)) {
            var c = 0
            for (ch in ranks[r]) {
                if (c >= 8) break
                if (ch.isDigit()) {
                    c += ch.digitToInt()
                } else {
                    val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    val type = PieceType.fromChar(ch)
                    if (type != null) {
                        squares[r * 8 + c] = ChessPiece(type, color)
                    }
                    c++
                }
            }
        }

        val epSq = if (epPart != "-") algebraicToSquare(epPart).takeIf { it in 0..63 } else null

        return ChessBoardState(
            squares = squares,
            sideToMove = PieceColor.fromCode(turnPart.firstOrNull() ?: 'w'),
            castlingWhiteKingside = castlingPart.contains('K'),
            castlingWhiteQueenside = castlingPart.contains('Q'),
            castlingBlackKingside = castlingPart.contains('k'),
            castlingBlackQueenside = castlingPart.contains('q'),
            enPassantSquare = epSq,
            halfMoveClock = halfMove,
            fullMoveNumber = fullMove
        )
    }

    fun toFen(state: ChessBoardState): String {
        val sb = StringBuilder()
        for (r in 0 until 8) {
            var emptyCount = 0
            for (c in 0 until 8) {
                val p = state.squares[r * 8 + c]
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (r < 7) sb.append('/')
        }

        sb.append(' ').append(state.sideToMove.code).append(' ')

        val castling = buildString {
            if (state.castlingWhiteKingside) append('K')
            if (state.castlingWhiteQueenside) append('Q')
            if (state.castlingBlackKingside) append('k')
            if (state.castlingBlackQueenside) append('q')
        }.ifEmpty { "-" }
        sb.append(castling).append(' ')

        val ep = state.enPassantSquare?.let { squareToAlgebraic(it) } ?: "-"
        sb.append(ep).append(' ')
        sb.append(state.halfMoveClock).append(' ')
        sb.append(state.fullMoveNumber)

        return sb.toString()
    }

    fun isSquareAttacked(state: ChessBoardState, targetSq: Int, byColor: PieceColor): Boolean {
        val tr = squareToRow(targetSq)
        val tc = squareToCol(targetSq)

        // 1. Pawn attacks
        val pawnAttackRow = if (byColor == PieceColor.WHITE) tr + 1 else tr - 1
        if (pawnAttackRow in 0..7) {
            for (dc in intArrayOf(-1, 1)) {
                val pc = tc + dc
                if (pc in 0..7) {
                    val p = state.squares[rowColToSquare(pawnAttackRow, pc)]
                    if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
                }
            }
        }

        // 2. Knight attacks
        val knightMoves = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1),
            intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2),
            intArrayOf(2, -1), intArrayOf(2, 1)
        )
        for (d in knightMoves) {
            val r = tr + d[0]
            val c = tc + d[1]
            if (r in 0..7 && c in 0..7) {
                val p = state.squares[rowColToSquare(r, c)]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // 3. King attacks
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = tr + dr
                val c = tc + dc
                if (r in 0..7 && c in 0..7) {
                    val p = state.squares[rowColToSquare(r, c)]
                    if (p != null && p.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        // 4. Orthogonal rays (Rook / Queen)
        val orthoDirs = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        for (d in orthoDirs) {
            var r = tr + d[0]
            var c = tc + d[1]
            while (r in 0..7 && c in 0..7) {
                val p = state.squares[rowColToSquare(r, c)]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                r += d[0]
                c += d[1]
            }
        }

        // 5. Diagonal rays (Bishop / Queen)
        val diagDirs = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
        for (d in diagDirs) {
            var r = tr + d[0]
            var c = tc + d[1]
            while (r in 0..7 && c in 0..7) {
                val p = state.squares[rowColToSquare(r, c)]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                r += d[0]
                c += d[1]
            }
        }

        return false
    }

    fun findKingSquare(state: ChessBoardState, color: PieceColor): Int {
        for (sq in 0 until 64) {
            val p = state.squares[sq]
            if (p != null && p.color == color && p.type == PieceType.KING) return sq
        }
        return -1
    }

    fun isKingInCheck(state: ChessBoardState, color: PieceColor = state.sideToMove): Boolean {
        val kingSq = findKingSquare(state, color)
        if (kingSq == -1) return false
        return isSquareAttacked(state, kingSq, color.opposite)
    }

    fun generatePseudoLegalMoves(state: ChessBoardState): List<ChessMove> {
        val moves = ArrayList<ChessMove>(48)
        val us = state.sideToMove
        val them = us.opposite

        for (sq in 0 until 64) {
            val piece = state.squares[sq] ?: continue
            if (piece.color != us) continue
            val r = squareToRow(sq)
            val c = squareToCol(sq)

            when (piece.type) {
                PieceType.PAWN -> {
                    val dir = if (us == PieceColor.WHITE) -1 else 1
                    val startRow = if (us == PieceColor.WHITE) 6 else 1
                    val promoRow = if (us == PieceColor.WHITE) 0 else 7
                    val nextR = r + dir

                    if (nextR in 0..7) {
                        val oneStepSq = rowColToSquare(nextR, c)
                        if (state.squares[oneStepSq] == null) {
                            if (nextR == promoRow) {
                                for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                    moves.add(ChessMove(sq, oneStepSq, promotion = promo))
                                }
                            } else {
                                moves.add(ChessMove(sq, oneStepSq))
                                if (r == startRow) {
                                    val twoStepSq = rowColToSquare(r + dir * 2, c)
                                    if (state.squares[twoStepSq] == null) {
                                        moves.add(ChessMove(sq, twoStepSq))
                                    }
                                }
                            }
                        }

                        // Captures & En passant
                        for (dc in intArrayOf(-1, 1)) {
                            val nc = c + dc
                            if (nc in 0..7) {
                                val targetSq = rowColToSquare(nextR, nc)
                                val targetPiece = state.squares[targetSq]
                                if (targetPiece != null && targetPiece.color == them) {
                                    if (nextR == promoRow) {
                                        for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                            moves.add(ChessMove(sq, targetSq, promotion = promo, capturedPiece = targetPiece))
                                        }
                                    } else {
                                        moves.add(ChessMove(sq, targetSq, capturedPiece = targetPiece))
                                    }
                                } else if (state.enPassantSquare == targetSq) {
                                    val capturedPawnSq = rowColToSquare(r, nc)
                                    val epCaptured = state.squares[capturedPawnSq]
                                    moves.add(ChessMove(sq, targetSq, isEnPassant = true, capturedPiece = epCaptured))
                                }
                            }
                        }
                    }
                }

                PieceType.KNIGHT -> {
                    val deltas = arrayOf(
                        intArrayOf(-2, -1), intArrayOf(-2, 1),
                        intArrayOf(-1, -2), intArrayOf(-1, 2),
                        intArrayOf(1, -2), intArrayOf(1, 2),
                        intArrayOf(2, -1), intArrayOf(2, 1)
                    )
                    for (d in deltas) {
                        val nr = r + d[0]
                        val nc = c + d[1]
                        if (nr in 0..7 && nc in 0..7) {
                            val toSq = rowColToSquare(nr, nc)
                            val target = state.squares[toSq]
                            if (target == null || target.color == them) {
                                moves.add(ChessMove(sq, toSq, capturedPiece = target))
                            }
                        }
                    }
                }

                PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                    val dirs = mutableListOf<IntArray>()
                    if (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN) {
                        dirs.add(intArrayOf(-1, -1))
                        dirs.add(intArrayOf(-1, 1))
                        dirs.add(intArrayOf(1, -1))
                        dirs.add(intArrayOf(1, 1))
                    }
                    if (piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN) {
                        dirs.add(intArrayOf(-1, 0))
                        dirs.add(intArrayOf(1, 0))
                        dirs.add(intArrayOf(0, -1))
                        dirs.add(intArrayOf(0, 1))
                    }
                    for (d in dirs) {
                        var nr = r + d[0]
                        var nc = c + d[1]
                        while (nr in 0..7 && nc in 0..7) {
                            val toSq = rowColToSquare(nr, nc)
                            val target = state.squares[toSq]
                            if (target == null) {
                                moves.add(ChessMove(sq, toSq))
                            } else {
                                if (target.color == them) {
                                    moves.add(ChessMove(sq, toSq, capturedPiece = target))
                                }
                                break
                            }
                            nr += d[0]
                            nc += d[1]
                        }
                    }
                }

                PieceType.KING -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0..7 && nc in 0..7) {
                                val toSq = rowColToSquare(nr, nc)
                                val target = state.squares[toSq]
                                if (target == null || target.color == them) {
                                    moves.add(ChessMove(sq, toSq, capturedPiece = target))
                                }
                            }
                        }
                    }

                    // Castling
                    if (!isKingInCheck(state, us)) {
                        if (us == PieceColor.WHITE && sq == 60) {
                            if (state.castlingWhiteKingside &&
                                state.squares[61] == null &&
                                state.squares[62] == null &&
                                state.squares[63]?.type == PieceType.ROOK &&
                                !isSquareAttacked(state, 61, them) &&
                                !isSquareAttacked(state, 62, them)
                            ) {
                                moves.add(ChessMove(60, 62, isCastlesKingside = true))
                            }
                            if (state.castlingWhiteQueenside &&
                                state.squares[59] == null &&
                                state.squares[58] == null &&
                                state.squares[57] == null &&
                                state.squares[56]?.type == PieceType.ROOK &&
                                !isSquareAttacked(state, 59, them) &&
                                !isSquareAttacked(state, 58, them)
                            ) {
                                moves.add(ChessMove(60, 58, isCastlesQueenside = true))
                            }
                        } else if (us == PieceColor.BLACK && sq == 4) {
                            if (state.castlingBlackKingside &&
                                state.squares[5] == null &&
                                state.squares[6] == null &&
                                state.squares[7]?.type == PieceType.ROOK &&
                                !isSquareAttacked(state, 5, them) &&
                                !isSquareAttacked(state, 6, them)
                            ) {
                                moves.add(ChessMove(4, 6, isCastlesKingside = true))
                            }
                            if (state.castlingBlackQueenside &&
                                state.squares[3] == null &&
                                state.squares[2] == null &&
                                state.squares[1] == null &&
                                state.squares[0]?.type == PieceType.ROOK &&
                                !isSquareAttacked(state, 3, them) &&
                                !isSquareAttacked(state, 2, them)
                            ) {
                                moves.add(ChessMove(4, 2, isCastlesQueenside = true))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    fun applyMove(state: ChessBoardState, move: ChessMove): ChessBoardState {
        val nextSquares = state.squares.toMutableList()
        val movingPiece = nextSquares[move.from] ?: return state
        val us = movingPiece.color

        nextSquares[move.from] = null
        if (move.isEnPassant) {
            val epCapturedRow = squareToRow(move.from)
            val epCapturedCol = squareToCol(move.to)
            nextSquares[rowColToSquare(epCapturedRow, epCapturedCol)] = null
        }

        if (move.isCastlesKingside) {
            if (us == PieceColor.WHITE) {
                nextSquares[61] = nextSquares[63]
                nextSquares[63] = null
            } else {
                nextSquares[5] = nextSquares[7]
                nextSquares[7] = null
            }
        } else if (move.isCastlesQueenside) {
            if (us == PieceColor.WHITE) {
                nextSquares[59] = nextSquares[56]
                nextSquares[56] = null
            } else {
                nextSquares[3] = nextSquares[0]
                nextSquares[0] = null
            }
        }

        val finalPiece = if (move.promotion != null) {
            ChessPiece(move.promotion, us)
        } else {
            movingPiece
        }
        nextSquares[move.to] = finalPiece

        var wk = state.castlingWhiteKingside
        var wq = state.castlingWhiteQueenside
        var bk = state.castlingBlackKingside
        var bq = state.castlingBlackQueenside

        if (movingPiece.type == PieceType.KING) {
            if (us == PieceColor.WHITE) {
                wk = false
                wq = false
            } else {
                bk = false
                bq = false
            }
        }
        if (move.from == 63 || move.to == 63) wk = false
        if (move.from == 56 || move.to == 56) wq = false
        if (move.from == 7 || move.to == 7) bk = false
        if (move.from == 0 || move.to == 0) bq = false

        val epSquare = if (movingPiece.type == PieceType.PAWN && abs(squareToRow(move.to) - squareToRow(move.from)) == 2) {
            rowColToSquare((squareToRow(move.from) + squareToRow(move.to)) / 2, squareToCol(move.from))
        } else {
            null
        }

        val halfMove = if (movingPiece.type == PieceType.PAWN || move.capturedPiece != null || move.isEnPassant) {
            0
        } else {
            state.halfMoveClock + 1
        }

        val fullMove = if (us == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        return ChessBoardState(
            squares = nextSquares,
            sideToMove = us.opposite,
            castlingWhiteKingside = wk,
            castlingWhiteQueenside = wq,
            castlingBlackKingside = bk,
            castlingBlackQueenside = bq,
            enPassantSquare = epSquare,
            halfMoveClock = halfMove,
            fullMoveNumber = fullMove
        )
    }

    fun generateLegalMoves(state: ChessBoardState): List<ChessMove> {
        val us = state.sideToMove
        return generatePseudoLegalMoves(state).filter { m ->
            val after = applyMove(state, m)
            !isKingInCheck(after, us)
        }
    }

    fun moveToSan(state: ChessBoardState, move: ChessMove): String {
        val movingPiece = state.squares[move.from] ?: return move.uci
        val legalMoves = generateLegalMoves(state)
        val afterState = applyMove(state, move)
        val oppLegal = generateLegalMoves(afterState)
        val inCheck = isKingInCheck(afterState, afterState.sideToMove)
        val suffix = when {
            inCheck && oppLegal.isEmpty() -> "#"
            inCheck -> "+"
            else -> ""
        }

        if (move.isCastlesKingside) return "O-O$suffix"
        if (move.isCastlesQueenside) return "O-O-O$suffix"

        val isCapture = move.capturedPiece != null || move.isEnPassant
        val sb = StringBuilder()

        if (movingPiece.type == PieceType.PAWN) {
            if (isCapture) {
                sb.append(squareToAlgebraic(move.from)[0])
                sb.append('x')
            }
            sb.append(squareToAlgebraic(move.to))
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.sanLetter)
            }
        } else {
            sb.append(movingPiece.type.sanLetter)
            val sameTypeMoves = legalMoves.filter {
                it.from != move.from &&
                    it.to == move.to &&
                    state.squares[it.from]?.type == movingPiece.type
            }
            if (sameTypeMoves.isNotEmpty()) {
                val fromFile = squareToAlgebraic(move.from)[0]
                val fromRank = squareToAlgebraic(move.from)[1]
                val fileConflict = sameTypeMoves.any { squareToAlgebraic(it.from)[0] == fromFile }
                val rankConflict = sameTypeMoves.any { squareToAlgebraic(it.from)[1] == fromRank }
                when {
                    !fileConflict -> sb.append(fromFile)
                    !rankConflict -> sb.append(fromRank)
                    else -> sb.append(fromFile).append(fromRank)
                }
            }
            if (isCapture) sb.append('x')
            sb.append(squareToAlgebraic(move.to))
        }

        sb.append(suffix)
        return sb.toString()
    }

    fun parseUciMove(state: ChessBoardState, uci: String): ChessMove? {
        val clean = uci.trim().lowercase()
        if (clean.length < 4) return null
        val fromSq = algebraicToSquare(clean.substring(0, 2))
        val toSq = algebraicToSquare(clean.substring(2, 4))
        val promo = clean.getOrNull(4)?.let { PieceType.fromChar(it) }
        return generateLegalMoves(state).firstOrNull {
            it.from == fromSq && it.to == toSq && (promo == null || it.promotion == promo)
        }
    }

    fun parseSanMove(state: ChessBoardState, rawSan: String): ChessMove? {
        val clean = rawSan.trim()
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")
            .replace(Regex("[+#?!]+$"), "")
        if (clean.isEmpty()) return null

        val legalMoves = generateLegalMoves(state)
        for (m in legalMoves) {
            val candidateSan = moveToSan(state, m).replace(Regex("[+#?!]+$"), "")
            if (candidateSan == clean) return m
        }

        // Also allow UCI fallback
        parseUciMove(state, clean)?.let { return it }

        return null
    }

    fun parsePgn(pgnText: String): ParsedPgnGame {
        val headerRegex = Regex("\\[(\\w+)\\s+\"([^\"]*)\"\\]")
        val headerMap = mutableMapOf<String, String>()
        for (match in headerRegex.findAll(pgnText)) {
            headerMap[match.groupValues[1]] = match.groupValues[2]
        }

        val initialFen = headerMap["FEN"]?.takeIf { it.isNotBlank() } ?: INITIAL_FEN

        // Strip headers, comments {...}, and recursive variations (...)
        var moveSection = pgnText.replace(headerRegex, " ")
        moveSection = moveSection.replace(Regex("\\{[^}]*\\}"), " ")
        while (moveSection.contains("(")) {
            val stripped = moveSection.replace(Regex("\\([^()]*\\)"), " ")
            if (stripped == moveSection) break
            moveSection = stripped
        }
        moveSection = moveSection.replace(Regex("\\$\\d+"), " ")
        moveSection = moveSection.replace(Regex("\\d+\\.+"), " ")

        val rawTokens = moveSection.trim().split(Regex("\\s+")).filter { token ->
            token.isNotBlank() && token !in setOf("1-0", "0-1", "1/2-1/2", "*")
        }

        var currentBoard = parseFen(initialFen)
        val nodes = ArrayList<MoveTreeNode>()
        val sanMoves = ArrayList<String>()
        var parentId: String? = null

        for ((index, token) in rawTokens.withIndex()) {
            val move = parseSanMove(currentBoard, token) ?: break
            val canonicalSan = moveToSan(currentBoard, move)
            val beforeFen = currentBoard.toFen()
            val nextBoard = applyMove(currentBoard, move)
            val afterFen = nextBoard.toFen()
            val ply = index + 1
            val moveNum = (index / 2) + 1
            val nodeId = "ply_${ply}_${move.uci}"

            nodes.add(
                MoveTreeNode(
                    id = nodeId,
                    parentId = parentId,
                    ply = ply,
                    moveNumber = moveNum,
                    color = currentBoard.sideToMove,
                    san = canonicalSan,
                    uci = move.uci,
                    from = move.fromAlgebraic,
                    to = move.toAlgebraic,
                    fenBefore = beforeFen,
                    fenAfter = afterFen
                )
            )
            sanMoves.add(canonicalSan)
            parentId = nodeId
            currentBoard = nextBoard
        }

        val (detectedEco, detectedOpening) = EcoOpeningBook.detectOpening(sanMoves)

        val headers = GameHeaders(
            event = headerMap["Event"] ?: "Chess Match",
            site = headerMap["Site"] ?: "Chess Insight Analyzer",
            date = headerMap["Date"] ?: "2025.05.01",
            round = headerMap["Round"] ?: "-",
            white = headerMap["White"] ?: "White",
            black = headerMap["Black"] ?: "Black",
            result = headerMap["Result"] ?: "*",
            whiteElo = headerMap["WhiteElo"] ?: "1800",
            blackElo = headerMap["BlackElo"] ?: "1800",
            timeControl = headerMap["TimeControl"] ?: "180+2",
            eco = headerMap["ECO"]?.takeIf { it.isNotBlank() } ?: detectedEco,
            opening = headerMap["Opening"]?.takeIf { it.isNotBlank() } ?: detectedOpening
        )

        return ParsedPgnGame(
            headers = headers,
            initialFen = initialFen,
            nodes = nodes,
            sanMoves = sanMoves
        )
    }

    /**
     * Computes captured pieces and net material balance from a FEN position.
     */
    fun computeCapturedMaterial(fen: String): CapturedMaterialInfo {
        val board = parseFen(fen)
        val initialCounts = mapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        val whiteCounts = mutableMapOf<PieceType, Int>()
        val blackCounts = mutableMapOf<PieceType, Int>()
        var whitePoints = 0
        var blackPoints = 0

        for (sq in 0 until 64) {
            val p = board.squares[sq] ?: continue
            val pts = when (p.type) {
                PieceType.PAWN -> 1
                PieceType.KNIGHT -> 3
                PieceType.BISHOP -> 3
                PieceType.ROOK -> 5
                PieceType.QUEEN -> 9
                PieceType.KING -> 0
            }
            if (p.color == PieceColor.WHITE) {
                whiteCounts[p.type] = (whiteCounts[p.type] ?: 0) + 1
                whitePoints += pts
            } else {
                blackCounts[p.type] = (blackCounts[p.type] ?: 0) + 1
                blackPoints += pts
            }
        }

        val capturedByWhite = mutableListOf<ChessPiece>()
        val capturedByBlack = mutableListOf<ChessPiece>()

        for ((type, startCount) in initialCounts) {
            val missingBlack = (startCount - (blackCounts[type] ?: 0)).coerceAtLeast(0)
            repeat(missingBlack) {
                capturedByWhite.add(ChessPiece(type, PieceColor.BLACK))
            }
            val missingWhite = (startCount - (whiteCounts[type] ?: 0)).coerceAtLeast(0)
            repeat(missingWhite) {
                capturedByBlack.add(ChessPiece(type, PieceColor.WHITE))
            }
        }

        return CapturedMaterialInfo(
            capturedByWhite = capturedByWhite,
            capturedByBlack = capturedByBlack,
            whiteAdvantage = (whitePoints - blackPoints).coerceAtLeast(0),
            blackAdvantage = (blackPoints - whitePoints).coerceAtLeast(0)
        )
    }

    data class CapturedMaterialInfo(
        val capturedByWhite: List<ChessPiece>,
        val capturedByBlack: List<ChessPiece>,
        val whiteAdvantage: Int,
        val blackAdvantage: Int
    )
}
