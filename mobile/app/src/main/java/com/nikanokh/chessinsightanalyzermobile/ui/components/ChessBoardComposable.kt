package com.nikanokh.chessinsightanalyzermobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessPosition
import com.nikanokh.chessinsightanalyzermobile.domain.model.*
import com.nikanokh.chessinsightanalyzermobile.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EcoOpeningBadgeRow(
    opening: OpeningInfo,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = ChessGoldEco.copy(alpha = 0.18f),
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier.border(
                width = 1.dp,
                color = ChessGoldEco.copy(alpha = 0.4f),
                shape = RoundedCornerShape(5.dp)
            )
        ) {
            Text(
                text = opening.eco,
                color = ChessGoldEco,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Text(
            text = opening.fullDisplayName,
            color = ChessTextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun GameHeaderCard(
    metadata: GameMetadata,
    opening: OpeningInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChessSurface)
            .border(1.dp, ChessBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = buildAnnotatedString {
                    append("${metadata.whiteName} vs ${metadata.blackName} ")
                    withStyle(SpanStyle(color = ChessGreenPrimary, fontWeight = FontWeight.Bold)) {
                        append("(${metadata.result})")
                    }
                },
                color = ChessTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            Text(
                text = metadata.date,
                color = ChessTextSecondary,
                fontSize = 11.sp
            )
        }

        EcoOpeningBadgeRow(opening = opening)
    }
}

@Composable
fun InteractiveBoardWithEvalBar(
    position: ChessPosition,
    currentNode: MoveNode?,
    selectedSquare: Square?,
    legalDestinations: Set<Square>,
    settings: AppSettings,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val evalBarWidth: Dp = if (settings.showEvalBar) 18.dp else 0.dp
        val gap: Dp = if (settings.showEvalBar) 6.dp else 0.dp
        val availableBoardWidth = (maxWidth - evalBarWidth - gap).coerceIn(200.dp, 430.dp)
        val squareSize = availableBoardWidth / 8f

        val evalCp = currentNode?.analysis?.evalAfterCp ?: 0
        val mateIn = currentNode?.analysis?.mateAfter
        val whiteFraction = when {
            mateIn != null -> if (mateIn > 0) 1f else 0f
            else -> (0.5f + (evalCp.coerceIn(-1000, 1000) / 1000f) * 0.45f).coerceIn(0.05f, 0.95f)
        }
        val evalLabel = when {
            mateIn != null -> "M${kotlin.math.abs(mateIn)}"
            else -> {
                val pawns = evalCp / 100.0
                if (pawns > 0) "+%.1f".format(pawns) else "%.1f".format(pawns)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            if (settings.showEvalBar) {
                Box(
                    modifier = Modifier
                        .width(evalBarWidth)
                        .height(availableBoardWidth)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF262421))
                        .border(1.dp, ChessBorder, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(whiteFraction)
                            .align(Alignment.BottomCenter)
                            .background(Color(0xFFEBEBEB))
                    )
                    Text(
                        text = evalLabel,
                        color = ChessGreenPrimary,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 3.dp)
                    )
                }
            }

            // 8x8 Interactive Chessboard + Best Move Arrow Overlay
            val isWhiteBottom = settings.boardOrientationWhite
            val ranks = if (isWhiteBottom) (7 downTo 0).toList() else (0..7).toList()
            val files = if (isWhiteBottom) (0..7).toList() else (7 downTo 0).toList()

            val lightColor = Color(settings.lightSquareHex)
            val darkColor = Color(settings.darkSquareHex)

            Box(
                modifier = Modifier
                    .size(availableBoardWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, ChessBorder, RoundedCornerShape(4.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    for ((rVisualIdx, rank) in ranks.withIndex()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(squareSize)
                        ) {
                            for ((fVisualIdx, file) in files.withIndex()) {
                                val sq = Square(file, rank)
                                val isLight = sq.isLightSquare
                                val isSelected = selectedSquare == sq
                                val isLastMove =
                                    currentNode?.fromSquare == sq.algebraic ||
                                        currentNode?.toSquare == sq.algebraic
                                val isLegalDest = settings.highlightLegalMoves && (sq in legalDestinations)
                                val piece = position.pieceAt(sq)

                                val bg = when {
                                    isSelected -> Color(0xFFBBCB2B)
                                    isLastMove -> if (isLight) Color(0xFFF7F769) else Color(0xFFBBCB2B)
                                    isLight -> lightColor
                                    else -> darkColor
                                }

                                Box(
                                    modifier = Modifier
                                        .size(squareSize)
                                        .background(bg)
                                        .clickable { onSquareClick(sq) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (fVisualIdx == 0) {
                                        Text(
                                            text = "${rank + 1}",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLight) darkColor else lightColor,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(start = 2.dp, top = 1.dp)
                                        )
                                    }
                                    if (rVisualIdx == 7) {
                                        Text(
                                            text = "${'a' + file}",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLight) darkColor else lightColor,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(end = 2.dp, bottom = 1.dp)
                                        )
                                    }

                                    if (piece != null) {
                                        Text(
                                            text = piece.unicode,
                                            fontSize = (squareSize.value * 0.68f).sp,
                                            color = if (piece.color == PieceColor.WHITE) {
                                                Color(0xFFFAFAFA)
                                            } else {
                                                Color(0xFF1A1917)
                                            }
                                        )
                                    }

                                    if (isLegalDest) {
                                        Box(
                                            modifier = Modifier
                                                .size(squareSize * 0.28f)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.28f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Engine Best Move Arrow Overlay
                val bestUci = currentNode?.analysis?.bestMoveUci
                if (settings.showEngineArrows && !bestUci.isNullOrBlank() && bestUci.length >= 4) {
                    val fromSq = Square.fromAlgebraic(bestUci.substring(0, 2))
                    val toSq = Square.fromAlgebraic(bestUci.substring(2, 4))
                    if (fromSq != null && toSq != null && fromSq != toSq) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cellPx = size.width / 8f
                            fun centerOf(sq: Square): Offset {
                                val col = if (isWhiteBottom) sq.file else (7 - sq.file)
                                val row = if (isWhiteBottom) (7 - sq.rank) else sq.rank
                                return Offset((col + 0.5f) * cellPx, (row + 0.5f) * cellPx)
                            }

                            val start = centerOf(fromSq)
                            val end = centerOf(toSq)
                            val arrowColor = Color(0xAA779954)

                            drawLine(
                                color = arrowColor,
                                start = start,
                                end = end,
                                strokeWidth = cellPx * 0.16f,
                                cap = StrokeCap.Round
                            )

                            val angle = atan2(end.y - start.y, end.x - start.x)
                            val headLen = cellPx * 0.36f
                            val p1 = Offset(
                                (end.x - headLen * cos(angle - 0.55)).toFloat(),
                                (end.y - headLen * sin(angle - 0.55)).toFloat()
                            )
                            val p2 = Offset(
                                (end.x - headLen * cos(angle + 0.55)).toFloat(),
                                (end.y - headLen * sin(angle + 0.55)).toFloat()
                            )
                            val path = Path().apply {
                                moveTo(end.x, end.y)
                                lineTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                close()
                            }
                            drawPath(path = path, color = arrowColor)
                        }
                    }
                }
            }
        }
    }
}
