package com.nikanokh.chessinsightanalyzermobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessBoardState
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessEngine
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessMove
import com.nikanokh.chessinsightanalyzermobile.domain.model.AppSettings
import com.nikanokh.chessinsightanalyzermobile.domain.model.ChessPiece
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineEvaluation
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineLine
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceType
import com.nikanokh.chessinsightanalyzermobile.domain.model.SquareHighlight
import com.nikanokh.chessinsightanalyzermobile.domain.model.UserArrow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun parseHexColor(hex: String, fallback: Color = Color(0xFF10B981)): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

/**
 * Renders a chess piece using either:
 * 1. `"neo"`: The original PNG bitmap asset from `/public/assets/aistudio/` (`R.drawable.piece_*`)
 * 2. `"classic_svg"`: The vector SVG paths ported 1:1 from Web `src/components/ChessBoard/CustomPieces.tsx`
 */
@Composable
fun ChessPieceImageComposable(
    piece: ChessPiece,
    pieceSet: String,
    modifier: Modifier = Modifier
) {
    if (pieceSet == "classic_svg") {
        ClassicSvgPieceComposable(piece = piece, modifier = modifier)
    } else {
        Image(
            painter = painterResource(id = piece.drawableRes),
            contentDescription = piece.assetKey,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

/**
 * 1:1 port of Web `ClassicSvgPiece` (`src/components/ChessBoard/CustomPieces.tsx`)
 */
@Composable
fun ClassicSvgPieceComposable(
    piece: ChessPiece,
    modifier: Modifier = Modifier
) {
    val isWhite = piece.color == PieceColor.WHITE
    val fillColor = if (isWhite) Color(0xFFF8FAFC) else Color(0xFF1E293B)
    val strokeColor = if (isWhite) Color(0xFF1E293B) else Color(0xFF0F172A)
    val innerStrokeColor = if (isWhite) Color(0xFF334155) else Color(0xFF94A3B8)

    Canvas(modifier = modifier) {
        val scaleX = size.width / 45f
        val scaleY = size.height / 45f
        val strokePx = 1.8f * scaleX

        fun drawSvgPath(pathData: String, fill: Boolean = true, stroke: Color = strokeColor, widthScale: Float = 1f) {
            val path = PathParser().parsePathString(pathData).toPath()
            val matrix = androidx.compose.ui.graphics.Matrix()
            matrix.scale(scaleX, scaleY)
            path.transform(matrix)
            if (fill) {
                drawPath(path = path, color = fillColor)
            }
            drawPath(
                path = path,
                color = stroke,
                style = Stroke(
                    width = strokePx * widthScale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        when (piece.type) {
            PieceType.PAWN -> {
                drawSvgPath("M 22.5,9 C 19.5,9 17,11.5 17,14.5 C 17,16.8 18.4,18.7 20.4,19.5 C 17,21 15,24.5 15,29 C 15,31 19,31 22.5,31 C 26,31 30,31 30,29 C 30,24.5 28,21 24.6,19.5 C 26.6,18.7 28,16.8 28,14.5 C 28,11.5 25.5,9 22.5,9 z")
                drawSvgPath("M 12.5,37 C 18,34 27,34 32.5,37 L 32.5,39.5 L 12.5,39.5 L 12.5,37 z")
            }
            PieceType.ROOK -> {
                drawSvgPath("M 9,39 L 36,39 L 36,36 L 9,36 L 9,39 z")
                drawSvgPath("M 12,36 L 12,32 L 33,32 L 33,36 L 12,36 z")
                drawSvgPath("M 14,29.5 L 14,16.5 L 31,16.5 L 31,29.5 L 14,29.5 z")
                drawSvgPath("M 14,16.5 L 11,14 L 34,14 L 31,16.5 L 14,16.5 z")
                drawSvgPath("M 11,14 L 11,9 L 15,9 L 15,11 L 20,11 L 20,9 L 25,9 L 25,11 L 30,11 L 30,9 L 34,9 L 34,14")
            }
            PieceType.KNIGHT -> {
                drawSvgPath("M 22,10 C 32.5,11 38.5,18 38,39 L 15,39 C 15,30 25,32.5 23,18")
                drawSvgPath("M 24,18 C 24.38,20.91 18.45,25.37 16,27 C 13,29 13.18,31.34 11,31 C 9.958,30.06 12.41,27.96 11,28 C 10,28 11.19,29.23 10,30 C 9,30 5.997,31 6,26 C 6,24 12,14 12,14 C 12,14 13.89,12.1 14,10.5 C 13.27,9.506 13.5,8.5 13.5,7.5 C 14.5,6.5 16.5,10 16.5,10 L 18.5,10 C 18.5,10 19.28,8.008 21,7 C 22,7 22,10 22,10")
                drawCircle(
                    color = innerStrokeColor,
                    radius = 1.5f * scaleX,
                    center = Offset(15.5f * scaleX, 14.5f * scaleY)
                )
            }
            PieceType.BISHOP -> {
                drawSvgPath("M 9,36 C 12.39,35.03 19.11,36.43 22.5,34 C 25.89,36.43 32.61,35.03 36,36 C 36,36 37.65,36.54 39,38 C 38.32,38.97 37.35,38.99 36,38.5 C 32.61,37.53 25.89,38.96 22.5,37.5 C 19.11,38.96 12.39,37.53 9,38.5 C 7.646,38.99 6.677,38.97 6,38 C 7.354,36.06 9,36 9,36 z")
                drawSvgPath("M 15,32 C 17.5,34.5 27.5,34.5 30,32 C 30.5,30.5 30,30 30,30 C 30,27.5 27.5,26 27.5,26 C 33,24.5 33.5,14.5 22.5,10.5 C 11.5,14.5 12,24.5 17.5,26 C 17.5,26 15,27.5 15,30 C 15,30 14.5,30.5 15,32 z")
                drawCircle(
                    color = fillColor,
                    radius = 2.5f * scaleX,
                    center = Offset(22.5f * scaleX, 8f * scaleY)
                )
                drawCircle(
                    color = strokeColor,
                    radius = 2.5f * scaleX,
                    center = Offset(22.5f * scaleX, 8f * scaleY),
                    style = Stroke(width = strokePx)
                )
                drawSvgPath("M 17.5,26 L 27.5,26 M 15,30 L 30,30 M 22.5,15.5 L 22.5,20.5 M 20,18 L 25,18", fill = false, stroke = innerStrokeColor, widthScale = 0.8f)
            }
            PieceType.QUEEN -> {
                drawSvgPath("M 9,26 C 17.5,24.5 30,24.5 36,26 L 38.5,13.5 L 31,25 L 30.7,10.9 L 25.5,24.5 L 22.5,10 L 19.5,24.5 L 14.3,10.9 L 14,25 L 6.5,13.5 L 9,26 z")
                drawSvgPath("M 9,26 C 9,28 10.5,28 11.5,30 C 12.5,31.5 12.5,31 12,33.5 C 10.5,34.5 11,36 11,36 C 9.5,37.5 11,38.5 11,38.5 C 17.5,39.5 27.5,39.5 34,38.5 C 34,38.5 35.5,37.5 34,36 C 34,36 34.5,34.5 33,33.5 C 32.5,31 32.5,31.5 33.5,30 C 34.5,28 36,28 36,26 C 27.5,24.5 17.5,24.5 9,26 z")
                val pearls = listOf(6f to 12f, 14f to 9f, 22.5f to 8f, 31f to 9f, 39f to 12f)
                for ((cx, cy) in pearls) {
                    drawCircle(color = fillColor, radius = 2f * scaleX, center = Offset(cx * scaleX, cy * scaleY))
                    drawCircle(color = strokeColor, radius = 2f * scaleX, center = Offset(cx * scaleX, cy * scaleY), style = Stroke(width = strokePx * 0.8f))
                }
            }
            PieceType.KING -> {
                drawSvgPath("M 22.5,11.63 L 22.5,6", fill = false)
                drawSvgPath("M 20,8 L 25,8", fill = false)
                drawSvgPath("M 22.5,25 C 22.5,25 27,17.5 25.5,14.5 C 25.5,14.5 24.5,12 22.5,12 C 20.5,12 19.5,14.5 19.5,14.5 C 18,17.5 22.5,25 22.5,25")
                drawSvgPath("M 11.5,37 C 17,40.5 27,40.5 32.5,37 L 32.5,30 C 32.5,30 41.5,25.5 38.5,19.5 C 34.5,13 25,16 22.5,23.5 L 22.5,27 L 22.5,23.5 C 19,16 9.5,13 6.5,19.5 C 3.5,25.5 11.5,29.5 11.5,29.5 L 11.5,37 z")
                drawSvgPath("M 11.5,30 C 17,27 27,27 32.5,30 M 11.5,33.5 C 17,30.5 27,30.5 32.5,33.5 M 11.5,37 C 17,34 27,34 32.5,37", fill = false, stroke = innerStrokeColor, widthScale = 0.8f)
            }
        }
    }
}

/**
 * Interactive 8x8 Chessboard matching Web `src/components/ChessBoard/ChessBoardView.tsx` 1:1.
 * Supports:
 * - Original PNG (`neo`) & Vector SVG (`classic_svg`) piece sets
 * - Tap-to-move AND Drag-and-Drop (`detectDragGestures`)
 * - Pawn promotion modal picker (`Q`, `R`, `B`, `N`)
 * - Last move highlight, King-in-check radial red glow, legal move dots & capture rings
 * - Destination square move classification icon badge (`R.drawable.cat_*`)
 * - MultiPV Engine Arrows (PV1 primary color, PV2+ secondary color) + User Arrows & Square Highlights
 */
@Composable
fun ChessBoardComposable(
    boardState: ChessBoardState,
    selectedSquare: Int?,
    legalMoves: List<ChessMove>,
    lastMoveFrom: String?,
    lastMoveTo: String?,
    lastMoveCategory: MoveCategory?,
    topLines: List<EngineLine> = emptyList(),
    bestMoveUci: String? = null,
    userArrows: List<UserArrow> = emptyList(),
    squareHighlights: List<SquareHighlight> = emptyList(),
    pendingPromotionMove: Pair<Int, Int>? = null,
    isFlipped: Boolean,
    settings: AppSettings,
    onSquareClick: (Int) -> Unit,
    onPieceDraggedMove: (Int, Int) -> Unit = { _, _ -> },
    onSelectPromotion: (PieceType) -> Unit = {},
    onCancelPromotion: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lightColor = Color(settings.lightSquareColorHex)
    val darkColor = Color(settings.darkSquareColorHex)
    val lastMoveColor = Color(0x94F5F682) // matches Web rgba(245, 246, 130, 0.58)
    val selectedColor = Color(0x80FFFF33)

    val lastFromSq = lastMoveFrom?.let { ChessEngine.algebraicToSquare(it) } ?: -1
    val lastToSq = lastMoveTo?.let { ChessEngine.algebraicToSquare(it) } ?: -1

    val legalTargetSquares = remember(selectedSquare, legalMoves) {
        if (selectedSquare == null) emptySet()
        else legalMoves.filter { it.from == selectedSquare }.map { it.to }.toSet()
    }

    val kingInCheckSq = remember(boardState) {
        if (ChessEngine.isKingInCheck(boardState, boardState.sideToMove)) {
            ChessEngine.findKingSquare(boardState, boardState.sideToMove)
        } else -1
    }

    val highlightSquaresMap = remember(squareHighlights) {
        squareHighlights.associate { ChessEngine.algebraicToSquare(it.square) to it.colorHex }
    }

    var draggingFromSq by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var dragCurrentTouchPx by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFF3F3D39), RoundedCornerShape(8.dp))
    ) {
        val boardSizeDp = maxWidth
        val squareSizeDp = boardSizeDp / 8f
        val density = LocalDensity.current
        val boardPx = with(density) { boardSizeDp.toPx() }
        val squarePx = boardPx / 8f

        fun offsetToSquare(offset: Offset): Int {
            val vCol = (offset.x / squarePx).toInt().coerceIn(0, 7)
            val vRow = (offset.y / squarePx).toInt().coerceIn(0, 7)
            val actualRow = if (isFlipped) 7 - vRow else vRow
            val actualCol = if (isFlipped) 7 - vCol else vCol
            return actualRow * 8 + actualCol
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped, boardPx) {
                    detectTapGestures { tapOffset ->
                        val sq = offsetToSquare(tapOffset)
                        if (sq in 0..63) {
                            onSquareClick(sq)
                        }
                    }
                }
                .pointerInput(isFlipped, boardPx, boardState) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val sq = offsetToSquare(startOffset)
                            val piece = boardState.pieceAt(sq)
                            if (sq in 0..63 && (piece != null || settings.showUserArrows)) {
                                draggingFromSq = sq
                                dragCurrentTouchPx = startOffset
                                dragOffsetPx = startOffset
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragCurrentTouchPx = change.position
                            dragOffsetPx = change.position
                        },
                        onDragEnd = {
                            val fromSq = draggingFromSq
                            if (fromSq != null) {
                                val toSq = offsetToSquare(dragCurrentTouchPx)
                                if (toSq in 0..63 && toSq != fromSq) {
                                    onPieceDraggedMove(fromSq, toSq)
                                }
                            }
                            draggingFromSq = null
                        },
                        onDragCancel = {
                            draggingFromSq = null
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (visualRow in 0 until 8) {
                    val actualRow = if (isFlipped) 7 - visualRow else visualRow
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (visualCol in 0 until 8) {
                            val actualCol = if (isFlipped) 7 - visualCol else visualCol
                            val sq = actualRow * 8 + actualCol
                            val isLight = (actualRow + actualCol) % 2 == 0
                            val baseSquareColor = if (isLight) lightColor else darkColor

                            val isSelected = selectedSquare == sq
                            val isLastMove = settings.highlightLastMove && (sq == lastFromSq || sq == lastToSq)
                            val isLegalDest = settings.highlightLegalMoves && legalTargetSquares.contains(sq)
                            val isKingCheck = sq == kingInCheckSq
                            val customHighlight = highlightSquaresMap[sq]
                            val piece = boardState.pieceAt(sq)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(baseSquareColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLastMove) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(lastMoveColor)
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(selectedColor)
                                    )
                                }
                                if (customHighlight != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0x73EF4444))
                                    )
                                }
                                if (isKingCheck) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xEBFF2D2D),
                                                        Color(0x99DC2626),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Rank coordinate (top-left of leftmost column)
                                if (settings.showCoordinates && visualCol == 0) {
                                    Text(
                                        text = "${8 - actualRow}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLight) darkColor else lightColor,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 2.dp, top = 1.dp)
                                    )
                                }

                                // File coordinate (bottom-right of bottom row)
                                if (settings.showCoordinates && visualRow == 7) {
                                    val fileChar = ('a'.code + actualCol).toChar()
                                    Text(
                                        text = fileChar.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLight) darkColor else lightColor,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 2.dp, bottom = 1.dp)
                                    )
                                }

                                // Piece image (dimmed slightly if currently being dragged)
                                if (piece != null && draggingFromSq != sq) {
                                    ChessPieceImageComposable(
                                        piece = piece,
                                        pieceSet = settings.pieceSet,
                                        modifier = Modifier
                                            .fillMaxSize(0.86f)
                                    )
                                }

                                // Legal move indicators (dot on empty square, ring on capture square)
                                if (isLegalDest) {
                                    if (piece == null) {
                                        Box(
                                            modifier = Modifier
                                                .size(squareSizeDp * 0.28f)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.24f))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.90f)
                                                .border(
                                                    width = (squareSizeDp * 0.09f).coerceAtLeast(2.5.dp),
                                                    color = Color.Black.copy(alpha = 0.28f),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                // Destination square Move Classification Badge (matching Web ChessBoardView.tsx)
                                if (sq == lastToSq && lastMoveCategory != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .size((squareSizeDp * 0.40f).coerceIn(14.dp, 24.dp))
                                            .shadow(3.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(lastMoveCategory.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = lastMoveCategory.iconRes),
                                            contentDescription = lastMoveCategory.labelRu,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Overlay Canvas for MultiPV Engine Arrows + User Arrows
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellWidth = size.width / 8f
                val cellHeight = size.height / 8f
                val strokeFactor = (settings.arrowWidth.coerceIn(14, 32) / 20f)
                val alphaVal = settings.arrowOpacity.coerceIn(0.4f, 1.0f)

                fun squareCenter(sq: Int): Offset {
                    val r = sq / 8
                    val c = sq % 8
                    val vr = if (isFlipped) 7 - r else r
                    val vc = if (isFlipped) 7 - c else c
                    return Offset((vc + 0.5f) * cellWidth, (vr + 0.5f) * cellHeight)
                }

                fun drawChessArrow(fromSq: Int, toSq: Int, rawColor: Color, isSecondary: Boolean = false) {
                    if (fromSq !in 0..63 || toSq !in 0..63 || fromSq == toSq) return
                    val start = squareCenter(fromSq)
                    val end = squareCenter(toSq)
                    val angle = atan2(end.y - start.y, end.x - start.x)
                    val headLen = cellWidth * (if (isSecondary) 0.30f else 0.36f) * strokeFactor
                    val lineEnd = Offset(
                        end.x - headLen * 0.55f * cos(angle),
                        end.y - headLen * 0.55f * sin(angle)
                    )
                    val color = rawColor.copy(alpha = if (isSecondary) alphaVal * 0.65f else alphaVal)

                    drawLine(
                        color = color,
                        start = start,
                        end = lineEnd,
                        strokeWidth = cellWidth * (if (isSecondary) 0.11f else 0.145f) * strokeFactor,
                        cap = StrokeCap.Round
                    )

                    val wingAngle = 0.55f
                    val p1 = Offset(
                        end.x - headLen * cos(angle - wingAngle),
                        end.y - headLen * sin(angle - wingAngle)
                    )
                    val p2 = Offset(
                        end.x - headLen * cos(angle + wingAngle),
                        end.y - headLen * sin(angle + wingAngle)
                    )
                    val path = Path().apply {
                        moveTo(end.x, end.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        close()
                    }
                    drawPath(path = path, color = color)
                }

                // 1. Engine MultiPV arrows (draw secondary lines first so PV1 is on top)
                if (settings.showEngineArrows) {
                    val primaryColor = parseHexColor(settings.engineArrowColorHex, Color(0xFF10B981))
                    val secondaryColor = parseHexColor(settings.secondaryEngineArrowColorHex, Color(0xFF3B82F6))

                    if (topLines.isNotEmpty()) {
                        for (line in topLines.sortedByDescending { it.multipv }) {
                            val uci = line.bestMoveUci
                            if (uci.length >= 4) {
                                val fSq = ChessEngine.algebraicToSquare(uci.substring(0, 2))
                                val tSq = ChessEngine.algebraicToSquare(uci.substring(2, 4))
                                val isPrimary = line.multipv == 1
                                drawChessArrow(
                                    fromSq = fSq,
                                    toSq = tSq,
                                    rawColor = if (isPrimary) primaryColor else secondaryColor,
                                    isSecondary = !isPrimary
                                )
                            }
                        }
                    } else if (!bestMoveUci.isNullOrBlank() && bestMoveUci.length >= 4) {
                        val fSq = ChessEngine.algebraicToSquare(bestMoveUci.substring(0, 2))
                        val tSq = ChessEngine.algebraicToSquare(bestMoveUci.substring(2, 4))
                        drawChessArrow(fSq, tSq, primaryColor, isSecondary = false)
                    }
                }

                // 2. User-drawn arrows
                if (settings.showUserArrows) {
                    for (ua in userArrows) {
                        val fSq = ChessEngine.algebraicToSquare(ua.from)
                        val tSq = ChessEngine.algebraicToSquare(ua.to)
                        val col = parseHexColor(ua.colorHex, Color(0xFFF59E0B))
                        drawChessArrow(fSq, tSq, col, isSecondary = false)
                    }
                }
            }

            // Floating dragged piece preview
            val activeDragSq = draggingFromSq
            if (activeDragSq != null) {
                val draggedPiece = boardState.pieceAt(activeDragSq)
                if (draggedPiece != null) {
                    val halfPx = (squarePx * 0.5f).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (dragOffsetPx.x.roundToInt() - halfPx),
                                    (dragOffsetPx.y.roundToInt() - halfPx)
                                )
                            }
                            .size(squareSizeDp * 1.05f)
                    ) {
                        ChessPieceImageComposable(
                            piece = draggedPiece,
                            pieceSet = settings.pieceSet,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Pawn Promotion Modal Picker (matches Web ChessBoardView.tsx promotion dialog)
            if (pendingPromotionMove != null) {
                val moverColor = boardState.sideToMove
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.62f))
                        .clickable { onCancelPromotion() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFF1F2937),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Выберите фигуру превращения",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val promoChoices = listOf(
                                    PieceType.QUEEN to "Ферзь",
                                    PieceType.ROOK to "Ладья",
                                    PieceType.BISHOP to "Слон",
                                    PieceType.KNIGHT to "Конь"
                                )
                                for ((pType, label) in promoChoices) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF374151))
                                            .clickable { onSelectPromotion(pType) }
                                            .padding(8.dp)
                                    ) {
                                        ChessPieceImageComposable(
                                            piece = ChessPiece(pType, moverColor),
                                            pieceSet = settings.pieceSet,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            color = Color(0xFFD1D5DB),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vertical Stockfish Evaluation Bar matching Web `EvaluationBar.tsx`.
 */
@Composable
fun ChessEvalBarComposable(
    evaluation: EngineEvaluation,
    isFlipped: Boolean,
    modifier: Modifier = Modifier
) {
    val whiteFraction = remember(evaluation) {
        if (evaluation.mate != null) {
            if (evaluation.mate > 0) 0.98f else 0.02f
        } else {
            val clamped = (evaluation.cp ?: 0).coerceIn(-1000, 1000)
            (0.5f + (clamped / 2000f)).coerceIn(0.04f, 0.96f)
        }
    }

    val scoreText = remember(evaluation) {
        if (evaluation.mate != null) {
            "M${abs(evaluation.mate)}"
        } else {
            val pawns = (evaluation.cp ?: 0) / 100.0
            if (pawns >= 0) "+%.1f".format(java.util.Locale.US, pawns)
            else "%.1f".format(java.util.Locale.US, pawns)
        }
    }

    Box(
        modifier = modifier
            .width(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF262421))
            .border(1.dp, Color(0xFF3F3D39), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val topWeight = if (isFlipped) whiteFraction else (1f - whiteFraction)
            val bottomWeight = 1f - topWeight

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(topWeight.coerceAtLeast(0.02f))
                    .background(if (isFlipped) Color(0xFFE5E7EB) else Color(0xFF262421))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(bottomWeight.coerceAtLeast(0.02f))
                    .background(if (isFlipped) Color(0xFF262421) else Color(0xFFE5E7EB))
            )
        }

        Text(
            text = scoreText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color(0xCC161512), RoundedCornerShape(3.dp))
                .padding(horizontal = 2.dp, vertical = 1.dp)
        )
    }
}
