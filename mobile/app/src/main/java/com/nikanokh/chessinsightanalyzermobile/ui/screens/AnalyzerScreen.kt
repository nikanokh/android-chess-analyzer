package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveNode
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.Square
import com.nikanokh.chessinsightanalyzermobile.ui.components.GameHeaderCard
import com.nikanokh.chessinsightanalyzermobile.ui.components.InteractiveBoardWithEvalBar
import com.nikanokh.chessinsightanalyzermobile.ui.theme.*
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessUiState

private data class MovePairRow(
    val moveNumber: Int,
    val white: MoveNode?,
    val black: MoveNode?
)

@Composable
fun AnalyzerScreen(
    uiState: ChessUiState,
    onSquareClick: (Square) -> Unit,
    onSelectPly: (Int) -> Unit,
    onGoStart: () -> Unit,
    onGoPrev: () -> Unit,
    onGoNext: () -> Unit,
    onGoEnd: () -> Unit,
    onRunAnalysis: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onFlipBoard: () -> Unit
) {
    var activeSubTab by remember { mutableStateOf("moves") }
    val moveListState = rememberLazyListState()

    val movePairs = remember(uiState.moves) {
        val list = mutableListOf<MovePairRow>()
        var i = 0
        while (i < uiState.moves.size) {
            val first = uiState.moves[i]
            if (first.color == PieceColor.WHITE) {
                val second = uiState.moves.getOrNull(i + 1)?.takeIf { it.color == PieceColor.BLACK }
                list.add(MovePairRow(first.moveNumber, first, second))
                i += if (second != null) 2 else 1
            } else {
                list.add(MovePairRow(first.moveNumber, null, first))
                i += 1
            }
        }
        list
    }

    // Auto-scroll move list when user makes a move on the interactive board or navigates moves
    LaunchedEffect(uiState.currentPly, uiState.lastBoardMoveScrollTick) {
        if (uiState.settings.autoScrollMoves && activeSubTab == "moves" && movePairs.isNotEmpty()) {
            val targetPairIdx = ((uiState.currentPly - 1).coerceAtLeast(0)) / 2
            if (targetPairIdx in movePairs.indices) {
                moveListState.animateScrollToItem(targetPairIdx)
            }
        }
    }

    val isWhiteBottom = uiState.settings.boardOrientationWhite
    val topPlayerName = if (isWhiteBottom) uiState.metadata.blackName else uiState.metadata.whiteName
    val topPlayerRating = if (isWhiteBottom) uiState.metadata.blackRating else uiState.metadata.whiteRating
    val bottomPlayerName = if (isWhiteBottom) uiState.metadata.whiteName else uiState.metadata.blackName
    val bottomPlayerRating = if (isWhiteBottom) uiState.metadata.whiteRating else uiState.metadata.blackRating

    val captured = uiState.capturedSummary
    val topMaterialAdv = if (isWhiteBottom && captured.materialDiffWhitePov < 0) {
        "+${kotlin.math.abs(captured.materialDiffWhitePov)}"
    } else if (!isWhiteBottom && captured.materialDiffWhitePov > 0) {
        "+${captured.materialDiffWhitePov}"
    } else {
        ""
    }

    val bottomMaterialAdv = if (isWhiteBottom && captured.materialDiffWhitePov > 0) {
        "+${captured.materialDiffWhitePov}"
    } else if (!isWhiteBottom && captured.materialDiffWhitePov < 0) {
        "+${kotlin.math.abs(captured.materialDiffWhitePov)}"
    } else {
        ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Game Title + Full Wrapping Opening Header
        GameHeaderCard(
            metadata = uiState.metadata,
            opening = uiState.opening
        )

        // 2. Top Player Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isWhiteBottom) Color(0xFF262421) else Color.White)
                        .border(1.dp, Color.Gray, RoundedCornerShape(3.dp))
                )
                Text(
                    text = buildString {
                        append(topPlayerName)
                        if (topPlayerRating.isNotBlank()) append(" ($topPlayerRating)")
                    },
                    color = ChessTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (topMaterialAdv.isNotEmpty()) {
                Text(
                    text = topMaterialAdv,
                    color = ChessGreenPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Slim Evaluation Bar + Interactive 8x8 Chessboard
        InteractiveBoardWithEvalBar(
            position = uiState.currentPosition,
            currentNode = uiState.currentNode,
            selectedSquare = uiState.selectedSquare,
            legalDestinations = uiState.legalDestinations,
            settings = uiState.settings,
            onSquareClick = onSquareClick
        )

        // 4. Bottom Player Strip + Current Move Classification Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isWhiteBottom) Color.White else Color(0xFF262421))
                        .border(1.dp, Color.Gray, RoundedCornerShape(3.dp))
                )
                Text(
                    text = buildString {
                        append(bottomPlayerName)
                        if (bottomPlayerRating.isNotBlank()) append(" ($bottomPlayerRating)")
                        if (bottomMaterialAdv.isNotEmpty()) append(" $bottomMaterialAdv")
                    },
                    color = ChessTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            uiState.currentNode?.analysis?.let { a ->
                val node = uiState.currentNode!!
                Text(
                    text = "${node.moveNumber}${if (node.color == PieceColor.WHITE) "." else "..."} ${node.san} · ${a.category.labelRu} ${a.category.badgeSymbol}",
                    color = a.category.composeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Desktop-Style Game Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TransportButton(text = "|<", onClick = onGoStart)
                TransportButton(text = "<", onClick = onGoPrev)
                TransportButton(text = ">", onClick = onGoNext)
                TransportButton(text = ">|", onClick = onGoEnd)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (uiState.isAnalyzing) {
                    Button(
                        onClick = onCancelAnalysis,
                        colors = ButtonDefaults.buttonColors(containerColor = ChessDanger),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "Стоп (${uiState.analysisCurrent}/${uiState.analysisTotal})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onRunAnalysis,
                        colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Анализ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TransportButton(text = "Переворот", onClick = onFlipBoard)
            }
        }

        // 6. Sub-Tabs: Ходы | Статистика
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(ChessSurface)
                .border(1.dp, ChessBorder, RoundedCornerShape(9.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SubTabChip(
                title = "Ходы (${uiState.moves.size})",
                selected = activeSubTab == "moves",
                onClick = { activeSubTab = "moves" },
                modifier = Modifier.weight(1f)
            )
            SubTabChip(
                title = "Статистика",
                selected = activeSubTab == "stats",
                onClick = { activeSubTab = "stats" },
                modifier = Modifier.weight(1f)
            )
        }

        // 7. Auto-Scrolling Move List or Statistics Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(ChessSurface)
                .border(1.dp, ChessBorder, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            if (activeSubTab == "moves") {
                if (movePairs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Сделайте ход на доске или загрузите партию PGN",
                            color = ChessTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = moveListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        itemsIndexed(movePairs) { _, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${pair.moveNumber}.",
                                    color = ChessTextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(34.dp)
                                )

                                MoveCellButton(
                                    node = pair.white,
                                    isActive = pair.white?.ply == uiState.currentPly,
                                    onClick = { pair.white?.let { onSelectPly(it.ply) } },
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                MoveCellButton(
                                    node = pair.black,
                                    isActive = pair.black?.ply == uiState.currentPly,
                                    onClick = { pair.black?.let { onSelectPly(it.ply) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                val stats = uiState.statistics
                if (stats == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Нажмите «Анализ», чтобы рассчитать точность и классификацию всех ходов.",
                            color = ChessTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                text = "⬜ ${uiState.metadata.whiteName}: ${stats.whiteAccuracy}% (${stats.whiteAvgCpl} сП)",
                                color = ChessTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⬛ ${uiState.metadata.blackName}: ${stats.blackAccuracy}% (${stats.blackAvgCpl} сП)",
                                color = ChessTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        itemsIndexed(MoveCategory.entries) { _, cat ->
                            val wCount = stats.whiteCategoryCounts[cat] ?: 0
                            val bCount = stats.blackCategoryCounts[cat] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$wCount",
                                    color = ChessTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = "${cat.labelRu} (${cat.badgeSymbol})",
                                    color = cat.composeColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$bCount",
                                    color = ChessTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(ChessSurface)
            .border(1.dp, ChessBorder, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ChessTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SubTabChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) ChessSurfaceElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) ChessGreenPrimary else ChessTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun MoveCellButton(
    node: MoveNode?,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (node == null) {
        Spacer(modifier = modifier)
        return
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) ChessGreenPrimary.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = node.san,
            color = ChessTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        node.analysis?.let { a ->
            Text(
                text = a.category.badgeSymbol,
                color = a.category.composeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
