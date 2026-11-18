package com.nikanokh.chessinsightanalyzermobile.domain.chess

import com.nikanokh.chessinsightanalyzermobile.domain.model.*
import kotlin.math.abs

const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

class ChessPosition(
    val board: Array<Array<Piece?>> = Array(8) { arrayOfNulls<Piece>(8) },
    var sideToMove: PieceColor = PieceColor.WHITE,
    var whiteCastleKingside: Boolean = true,
    var whiteCastleQueenside: Boolean = true,
    var blackCastleKingside: Boolean = true,
    var blackCastleQueenside: Boolean = true,
    var enPassantSquare: Square? = null,
    var halfmoveClock: Int = 0,
    var fullmoveNumber: Int = 1
) {
    fun copy(): ChessPosition {
        val nextBoard = Array(8) { f -> Array(8) { r -> board[f][r] } }
        return ChessPosition(
            board = nextBoard,
            sideToMove = sideToMove,
            whiteCastleKingside = whiteCastleKingside,
            whiteCastleQueenside = whiteCastleQueenside,
            blackCastleKingside = blackCastleKingside,
            blackCastleQueenside = blackCastleQueenside,
            enPassantSquare = enPassantSquare,
            halfmoveClock = halfmoveClock,
            fullmoveNumber = fullmoveNumber
        )
    }

    fun pieceAt(sq: Square): Piece? = board[sq.file][sq.rank]
    fun pieceAt(file: Int, rank: Int): Piece? =
        if (file in 0..7 && rank in 0..7) board[file][rank] else null

    fun setPiece(sq: Square, piece: Piece?) {
        board[sq.file][sq.rank] = piece
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val p = board[file][rank]
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    val ch = if (p.color == PieceColor.WHITE) {
                        p.type.fenChar.uppercaseChar()
                    } else {
                        p.type.fenChar
                    }
                    sb.append(ch)
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (rank > 0) sb.append('/')
        }
        sb.append(' ').append(sideToMove.code).append(' ')
        val castling = buildString {
            if (whiteCastleKingside) append('K')
            if (whiteCastleQueenside) append('Q')
            if (blackCastleKingside) append('k')
            if (blackCastleQueenside) append('q')
        }.ifEmpty { "-" }
        sb.append(castling).append(' ')
        sb.append(enPassantSquare?.algebraic ?: "-").append(' ')
        sb.append(halfmoveClock).append(' ').append(fullmoveNumber)
        return sb.toString()
    }

    fun findKing(color: PieceColor): Square? {
        for (f in 0..7) {
            for (r in 0..7) {
                val p = board[f][r]
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return Square(f, r)
                }
            }
        }
        return null
    }

    fun isSquareAttackedBy(target: Square, attackerColor: PieceColor): Boolean {
        // 1. Pawn attacks
        val pawnDir = if (attackerColor == PieceColor.WHITE) -1 else 1
        for (df in intArrayOf(-1, 1)) {
            val p = pieceAt(target.file + df, target.rank + pawnDir)
            if (p != null && p.color == attackerColor && p.type == PieceType.PAWN) return true
        }

        // 2. Knight attacks
        val knightJumps = arrayOf(
            intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
            intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
        )
        for (jump in knightJumps) {
            val p = pieceAt(target.file + jump[0], target.rank + jump[1])
            if (p != null && p.color == attackerColor && p.type == PieceType.KNIGHT) return true
        }

        // 3. King attacks
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val p = pieceAt(target.file + df, target.rank + dr)
                if (p != null && p.color == attackerColor && p.type == PieceType.KING) return true
            }
        }

        // 4. Orthogonal sliding (Rook / Queen)
        val orthoDirs = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
        for (dir in orthoDirs) {
            var f = target.file + dir[0]
            var r = target.rank + dir[1]
            while (f in 0..7 && r in 0..7) {
                val p = board[f][r]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }

        // 5. Diagonal sliding (Bishop / Queen)
        val diagDirs = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
        for (dir in diagDirs) {
            var f = target.file + dir[0]
            var r = target.rank + dir[1]
            while (f in 0..7 && r in 0..7) {
                val p = board[f][r]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }

        return false
    }

    fun isInCheck(color: PieceColor = sideToMove): Boolean {
        val kingSq = findKing(color) ?: return false
        return isSquareAttackedBy(kingSq, color.opposite())
    }

    fun generatePseudoLegalMoves(): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val us = sideToMove
        val them = us.opposite()

        for (f in 0..7) {
            for (r in 0..7) {
                val piece = board[f][r] ?: continue
                if (piece.color != us) continue
                val from = Square(f, r)

                when (piece.type) {
                    PieceType.PAWN -> {
                        val dir = if (us == PieceColor.WHITE) 1 else -1
                        val startRank = if (us == PieceColor.WHITE) 1 else 6
                        val promoRank = if (us == PieceColor.WHITE) 7 else 0
                        val nextRank = r + dir

                        if (nextRank in 0..7 && board[f][nextRank] == null) {
                            val to = Square(f, nextRank)
                            if (nextRank == promoRank) {
                                for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                    moves.add(ChessMove(from, to, piece, promotion = promo))
                                }
                            } else {
                                moves.add(ChessMove(from, to, piece))
                                val doubleRank = r + 2 * dir
                                if (r == startRank && board[f][doubleRank] == null) {
                                    moves.add(ChessMove(from, Square(f, doubleRank), piece))
                                }
                            }
                        }

                        for (df in intArrayOf(-1, 1)) {
                            val cf = f + df
                            if (cf !in 0..7 || nextRank !in 0..7) continue
                            val to = Square(cf, nextRank)
                            val target = board[cf][nextRank]
                            if (target != null && target.color == them) {
                                if (nextRank == promoRank) {
                                    for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                        moves.add(ChessMove(from, to, piece, captured = target, promotion = promo))
                                    }
                                } else {
                                    moves.add(ChessMove(from, to, piece, captured = target))
                                }
                            } else if (enPassantSquare == to) {
                                val epCaptured = board[cf][r] ?: Piece(PieceType.PAWN, them)
                                moves.add(ChessMove(from, to, piece, captured = epCaptured, isEnPassant = true))
                            }
                        }
                    }

                    PieceType.KNIGHT -> {
                        val jumps = arrayOf(
                            intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
                            intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
                        )
                        for (j in jumps) {
                            val nf = f + j[0]
                            val nr = r + j[1]
                            if (nf in 0..7 && nr in 0..7) {
                                val target = board[nf][nr]
                                if (target == null || target.color == them) {
                                    moves.add(ChessMove(from, Square(nf, nr), piece, captured = target))
                                }
                            }
                        }
                    }

                    PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                        val dirs = mutableListOf<IntArray>()
                        if (piece.type != PieceType.BISHOP) {
                            dirs.addAll(listOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)))
                        }
                        if (piece.type != PieceType.ROOK) {
                            dirs.addAll(listOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)))
                        }
                        for (d in dirs) {
                            var nf = f + d[0]
                            var nr = r + d[1]
                            while (nf in 0..7 && nr in 0..7) {
                                val target = board[nf][nr]
                                if (target == null) {
                                    moves.add(ChessMove(from, Square(nf, nr), piece))
                                } else {
                                    if (target.color == them) {
                                        moves.add(ChessMove(from, Square(nf, nr), piece, captured = target))
                                    }
                                    break
                                }
                                nf += d[0]
                                nr += d[1]
                            }
                        }
                    }

                    PieceType.KING -> {
                        for (df in -1..1) {
                            for (dr in -1..1) {
                                if (df == 0 && dr == 0) continue
                                val nf = f + df
                                val nr = r + dr
                                if (nf in 0..7 && nr in 0..7) {
                                    val target = board[nf][nr]
                                    if (target == null || target.color == them) {
                                        moves.add(ChessMove(from, Square(nf, nr), piece, captured = target))
                                    }
                                }
                            }
                        }

                        // Castling
                        val homeRank = if (us == PieceColor.WHITE) 0 else 7
                        if (f == 4 && r == homeRank && !isInCheck(us)) {
                            val canKingSide = if (us == PieceColor.WHITE) whiteCastleKingside else blackCastleKingside
                            if (canKingSide &&
                                board[5][homeRank] == null &&
                                board[6][homeRank] == null &&
                                board[7][homeRank]?.type == PieceType.ROOK &&
                                !isSquareAttackedBy(Square(5, homeRank), them) &&
                                !isSquareAttackedBy(Square(6, homeRank), them)
                            ) {
                                moves.add(ChessMove(from, Square(6, homeRank), piece, isKingsideCastle = true))
                            }

                            val canQueenSide = if (us == PieceColor.WHITE) whiteCastleQueenside else blackCastleQueenside
                            if (canQueenSide &&
                                board[3][homeRank] == null &&
                                board[2][homeRank] == null &&
                                board[1][homeRank] == null &&
                                board[0][homeRank]?.type == PieceType.ROOK &&
                                !isSquareAttackedBy(Square(3, homeRank), them) &&
                                !isSquareAttackedBy(Square(2, homeRank), them)
                            ) {
                                moves.add(ChessMove(from, Square(2, homeRank), piece, isQueensideCastle = true))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    fun applyMove(move: ChessMove): ChessPosition {
        val next = copy()
        val us = sideToMove
        val homeRank = if (us == PieceColor.WHITE) 0 else 7

        next.setPiece(move.from, null)
        if (move.isEnPassant) {
            next.board[move.to.file][move.from.rank] = null
        }

        val placedPiece = if (move.promotion != null) {
            Piece(move.promotion, us)
        } else {
            move.piece
        }
        next.setPiece(move.to, placedPiece)

        if (move.isKingsideCastle) {
            val rook = next.board[7][homeRank]
            next.board[7][homeRank] = null
            next.board[5][homeRank] = rook
        } else if (move.isQueensideCastle) {
            val rook = next.board[0][homeRank]
            next.board[0][homeRank] = null
            next.board[3][homeRank] = rook
        }

        // Update castling rights
        if (move.piece.type == PieceType.KING) {
            if (us == PieceColor.WHITE) {
                next.whiteCastleKingside = false
                next.whiteCastleQueenside = false
            } else {
                next.blackCastleKingside = false
                next.blackCastleQueenside = false
            }
        }
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) next.whiteCastleQueenside = false
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) next.whiteCastleKingside = false
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) next.blackCastleQueenside = false
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) next.blackCastleKingside = false

        // Update en passant square
        next.enPassantSquare = if (move.piece.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }

        next.halfmoveClock = if (move.piece.type == PieceType.PAWN || move.captured != null) {
            0
        } else {
            halfmoveClock + 1
        }

        if (us == PieceColor.BLACK) {
            next.fullmoveNumber = fullmoveNumber + 1
        }
        next.sideToMove = us.opposite()
        return next
    }

    fun generateLegalMoves(): List<ChessMove> {
        val us = sideToMove
        return generatePseudoLegalMoves().filter { candidate ->
            val after = applyMove(candidate)
            !after.isInCheck(us)
        }
    }

    fun toSan(move: ChessMove, legalMoves: List<ChessMove> = generateLegalMoves()): String {
        val after = applyMove(move)
        val suffix = if (after.isInCheck(after.sideToMove)) {
            if (after.generateLegalMoves().isEmpty()) "#" else "+"
        } else {
            ""
        }

        if (move.isKingsideCastle) return "O-O$suffix"
        if (move.isQueensideCastle) return "O-O-O$suffix"

        val sb = StringBuilder()
        if (move.piece.type == PieceType.PAWN) {
            if (move.captured != null) {
                sb.append('a' + move.from.file)
                sb.append('x')
            }
            sb.append(move.to.algebraic)
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.sanChar)
            }
        } else {
            sb.append(move.piece.type.sanChar)
            val ambiguous = legalMoves.filter {
                it != move &&
                    it.piece.type == move.piece.type &&
                    it.to == move.to
            }
            if (ambiguous.isNotEmpty()) {
                val sameFile = ambiguous.any { it.from.file == move.from.file }
                val sameRank = ambiguous.any { it.from.rank == move.from.rank }
                when {
                    !sameFile -> sb.append('a' + move.from.file)
                    !sameRank -> sb.append('1' + move.from.rank)
                    else -> sb.append(move.from.algebraic)
                }
            }
            if (move.captured != null) sb.append('x')
            sb.append(move.to.algebraic)
        }
        sb.append(suffix)
        return sb.toString()
    }

    fun parseSanMove(rawSan: String): ChessMove? {
        val legal = generateLegalMoves()
        if (legal.isEmpty()) return null

        val clean = rawSan.trim()
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")
            .replace(Regex("[+#?!]+$"), "")

        if (clean == "O-O") return legal.firstOrNull { it.isKingsideCastle }
        if (clean == "O-O-O") return legal.firstOrNull { it.isQueensideCastle }

        // Direct exact match against generated SANs
        for (m in legal) {
            val candidateSan = toSan(m, legal).replace(Regex("[+#?!]+$"), "")
            if (candidateSan == clean) return m
        }

        // Fallback flexible matcher
        var working = clean
        var promoType: PieceType? = null
        val promoIdx = working.indexOf('=')
        if (promoIdx != -1 && promoIdx + 1 < working.length) {
            promoType = when (working[promoIdx + 1].uppercaseChar()) {
                'Q' -> PieceType.QUEEN
                'R' -> PieceType.ROOK
                'B' -> PieceType.BISHOP
                'N' -> PieceType.KNIGHT
                else -> PieceType.QUEEN
            }
            working = working.substring(0, promoIdx)
        }

        if (working.length < 2) return null
        val targetSquare = Square.fromAlgebraic(working.takeLast(2)) ?: return null
        val prefix = working.dropLast(2).replace("x", "")

        val pieceType = if (prefix.isNotEmpty() && prefix[0].isUpperCase()) {
            when (prefix[0]) {
                'N' -> PieceType.KNIGHT
                'B' -> PieceType.BISHOP
                'R' -> PieceType.ROOK
                'Q' -> PieceType.QUEEN
                'K' -> PieceType.KING
                else -> PieceType.PAWN
            }
        } else {
            PieceType.PAWN
        }

        val disambig = if (pieceType == PieceType.PAWN) prefix else prefix.drop(1)
        var fileFilter: Int? = null
        var rankFilter: Int? = null
        for (ch in disambig) {
            if (ch in 'a'..'h') fileFilter = ch - 'a'
            if (ch in '1'..'8') rankFilter = ch - '1'
        }

        return legal.firstOrNull { m ->
            m.to == targetSquare &&
                m.piece.type == pieceType &&
                (promoType == null || m.promotion == promoType) &&
                (fileFilter == null || m.from.file == fileFilter) &&
                (rankFilter == null || m.from.rank == rankFilter)
        }
    }

    companion object {
        fun fromFen(fen: String = INITIAL_FEN): ChessPosition {
            val parts = fen.trim().split(Regex("\\s+"))
            val pos = ChessPosition(
                whiteCastleKingside = false,
                whiteCastleQueenside = false,
                blackCastleKingside = false,
                blackCastleQueenside = false
            )
            val rows = parts.getOrNull(0)?.split('/') ?: return fromFen(INITIAL_FEN)
            if (rows.size != 8) return fromFen(INITIAL_FEN)

            for (rIdx in 0..7) {
                val rank = 7 - rIdx
                var file = 0
                for (ch in rows[rIdx]) {
                    if (ch.isDigit()) {
                        file += (ch - '0')
                    } else {
                        val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                        val type = when (ch.lowercaseChar()) {
                            'p' -> PieceType.PAWN
                            'n' -> PieceType.KNIGHT
                            'b' -> PieceType.BISHOP
                            'r' -> PieceType.ROOK
                            'q' -> PieceType.QUEEN
                            'k' -> PieceType.KING
                            else -> PieceType.PAWN
                        }
                        if (file in 0..7) {
                            pos.board[file][rank] = Piece(type, color)
                        }
                        file++
                    }
                }
            }

            pos.sideToMove = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
            val castling = parts.getOrNull(2) ?: "-"
            pos.whiteCastleKingside = 'K' in castling
            pos.whiteCastleQueenside = 'Q' in castling
            pos.blackCastleKingside = 'k' in castling
            pos.blackCastleQueenside = 'q' in castling
            pos.enPassantSquare = parts.getOrNull(3)?.let { Square.fromAlgebraic(it) }
            pos.halfmoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0
            pos.fullmoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1
            return pos
        }
    }
}

object PgnParser {
    data class ParsedPgnGame(
        val metadata: GameMetadata,
        val opening: OpeningInfo,
        val moves: List<MoveNode>,
        val rawPgn: String,
        val finalFen: String
    )

    fun parsePgn(rawPgn: String, defaultPlatform: String = "PGN"): ParsedPgnGame {
        val headers = mutableMapOf<String, String>()
        val headerRegex = Regex("""\[(\w+)\s+"([^"]*)"\]""")
        for (match in headerRegex.findAll(rawPgn)) {
            headers[match.groupValues[1]] = match.groupValues[2]
        }

        // Strip headers and comments
        var body = rawPgn.replace(headerRegex, " ")
        body = body.replace(Regex("""\{[^}]*\}"""), " ")
        body = body.replace(Regex(""";[^\n]*"""), " ")
        // Strip nested variations (...)
        while (body.contains('(')) {
            val cleaned = body.replace(Regex("""\([^()]*\)"""), " ")
            if (cleaned == body) break
            body = cleaned
        }

        val tokens = body
            .replace(Regex("""\d+\.+"""), " ")
            .replace(Regex("""\$\d+"""), " ")
            .trim()
            .split(Regex("""\s+"""))
            .filter {
                it.isNotBlank() &&
                    it !in setOf("1-0", "0-1", "1/2-1/2", "*")
            }

        var pos = ChessPosition.fromFen(headers["FEN"] ?: INITIAL_FEN)
        val moves = mutableListOf<MoveNode>()

        for ((idx, token) in tokens.withIndex()) {
            val legal = pos.generateLegalMoves()
            val move = pos.parseSanMove(token) ?: break
            val san = pos.toSan(move, legal)
            val fenBefore = pos.toFen()
            val moveNumber = pos.fullmoveNumber
            val color = pos.sideToMove
            pos = pos.applyMove(move)
            val fenAfter = pos.toFen()

            moves.add(
                MoveNode(
                    id = "ply_${idx + 1}",
                    ply = idx + 1,
                    moveNumber = moveNumber,
                    color = color,
                    san = san,
                    uci = move.uci,
                    fromSquare = move.from.algebraic,
                    toSquare = move.to.algebraic,
                    fenBefore = fenBefore,
                    fenAfter = fenAfter
                )
            )
        }

        val detectedOpening = EcoOpeningBook.detectOpening(moves.map { it.san }, headers["ECO"])
        val metadata = GameMetadata(
            whiteName = headers["White"]?.takeIf { it.isNotBlank() } ?: "Белые",
            blackName = headers["Black"]?.takeIf { it.isNotBlank() } ?: "Чёрные",
            whiteRating = headers["WhiteElo"] ?: "",
            blackRating = headers["BlackElo"] ?: "",
            result = headers["Result"] ?: "*",
            date = headers["Date"] ?: "2025.02.25",
            event = headers["Event"] ?: "Шахматная партия",
            site = headers["Site"] ?: defaultPlatform,
            eco = detectedOpening.eco,
            platform = defaultPlatform
        )

        return ParsedPgnGame(
            metadata = metadata,
            opening = detectedOpening,
            moves = moves,
            rawPgn = rawPgn.trim(),
            finalFen = pos.toFen()
        )
    }

    fun buildPgn(metadata: GameMetadata, moves: List<MoveNode>): String {
        val sb = StringBuilder()
        sb.appendLine("""[Event "${metadata.event}"]""")
        sb.appendLine("""[Site "${metadata.site}"]""")
        sb.appendLine("""[Date "${metadata.date}"]""")
        sb.appendLine("""[White "${metadata.whiteName}"]""")
        sb.appendLine("""[Black "${metadata.blackName}"]""")
        sb.appendLine("""[Result "${metadata.result}"]""")
        sb.appendLine("""[ECO "${metadata.eco}"]""")
        sb.appendLine()

        val moveStr = buildString {
            for (node in moves) {
                if (node.color == PieceColor.WHITE) {
                    append("${node.moveNumber}. ${node.san} ")
                } else {
                    append("${node.san} ")
                }
            }
            append(metadata.result)
        }
        sb.append(moveStr.trim())
        return sb.toString()
    }

    fun calculateCapturedSummary(fen: String): CapturedSummary {
        val pos = ChessPosition.fromFen(fen)
        val initialCounts = mapOf(
            PieceType.PAWN to 8,
            PieceType.KNIGHT to 2,
            PieceType.BISHOP to 2,
            PieceType.ROOK to 2,
            PieceType.QUEEN to 1
        )
        val whiteOnBoard = mutableMapOf<PieceType, Int>()
        val blackOnBoard = mutableMapOf<PieceType, Int>()
        var whiteMaterial = 0
        var blackMaterial = 0

        for (f in 0..7) {
            for (r in 0..7) {
                val p = pos.board[f][r] ?: continue
                if (p.color == PieceColor.WHITE) {
                    whiteOnBoard[p.type] = (whiteOnBoard[p.type] ?: 0) + 1
                    whiteMaterial += p.type.materialValue
                } else {
                    blackOnBoard[p.type] = (blackOnBoard[p.type] ?: 0) + 1
                    blackMaterial += p.type.materialValue
                }
            }
        }

        val capturedByWhite = mutableListOf<PieceType>()
        val capturedByBlack = mutableListOf<PieceType>()
        for ((type, startCount) in initialCounts) {
            val missingBlack = (startCount - (blackOnBoard[type] ?: 0)).coerceAtLeast(0)
            repeat(missingBlack) { capturedByWhite.add(type) }

            val missingWhite = (startCount - (whiteOnBoard[type] ?: 0)).coerceAtLeast(0)
            repeat(missingWhite) { capturedByBlack.add(type) }
        }

        return CapturedSummary(
            capturedByWhite = capturedByWhite,
            capturedByBlack = capturedByBlack,
            materialDiffWhitePov = whiteMaterial - blackMaterial
        )
    }
}
