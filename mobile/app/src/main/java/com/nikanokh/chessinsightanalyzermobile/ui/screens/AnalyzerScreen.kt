package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.chess.ChessEngine
import com.nikanokh.chessinsightanalyzermobile.domain.model.ChessPiece
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineLine
import com.nikanokh.chessinsightanalyzermobile.domain.model.GameStatistics
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveAnalysis
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveTreeNode
import com.nikanokh.chessinsightanalyzermobile.domain.model.OpeningAnalysisSummary
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceType
import com.nikanokh.chessinsightanalyzermobile.ui.components.ChessBoardComposable
import com.nikanokh.chessinsightanalyzermobile.ui.components.ChessEvalBarComposable
import com.nikanokh.chessinsightanalyzermobile.ui.components.ChessPieceImageComposable
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGold
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGreen
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentTeal
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgCard
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgElevated
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessUiState
import kotlin.math.abs

@Composable
fun AnalyzerScreen(
    uiState: ChessUiState,
    onSquareClick: (Int) -> Unit,
    onPieceDraggedMove: (Int, Int) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onCancelPromotion: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onJumpToStart: () -> Unit,
    onJumpToEnd: () -> Unit,
    onJumpToPly: (Int) -> Unit,
    onFlipBoard: () -> Unit,
    onToggleAnnotationMode: () -> Unit,
    onClearAnnotations: () -> Unit,
    onRunAnalysis: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onSaveGame: () -> Unit,
    onStopPractice: () -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val currentMoveNode = uiState.moveNodes.getOrNull(uiState.currentPly - 1)
    val currentMoveAnalysis = uiState.analyses.getOrNull(uiState.currentPly - 1)
        ?: currentMoveNode?.analysis

    val capturedInfo = remember(uiState.currentFen) {
        ChessEngine.computeCapturedMaterial(uiState.currentFen)
    }

    val topPlayerIsBlack = !uiState.isFlipped
    val boardMaxHeightFraction = when (uiState.settings.boardSize) {
        "small" -> 0.80f
        "medium" -> 0.90f
        else -> 1.0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Active Opening Repertoire Practice Banner
        if (uiState.activePracticeEntry != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F3324)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Тренажёр дебютов: ${uiState.activePracticeEntry.title}",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.practiceFeedback ?: "Ваш ход по теории",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onStopPractice, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Выйти",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Opening Theory & ECO Header Bar (when showLeftSidebar is enabled)
        if (uiState.settings.showLeftSidebar) {
            OpeningSummaryStrip(
                openingSummary = uiState.openingSummary,
                result = uiState.headers.result
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Top Player Bar (Opponent)
        PlayerMaterialBar(
            name = if (topPlayerIsBlack) uiState.headers.black else uiState.headers.white,
            elo = if (topPlayerIsBlack) uiState.headers.blackElo else uiState.headers.whiteElo,
            isBlack = topPlayerIsBlack,
            capturedPieces = if (topPlayerIsBlack) capturedInfo.capturedByBlack else capturedInfo.capturedByWhite,
            materialAdvantage = if (topPlayerIsBlack) capturedInfo.blackAdvantage else capturedInfo.whiteAdvantage,
            accuracy = if (uiState.analyses.isNotEmpty()) {
                if (topPlayerIsBlack) uiState.gameStats.black.accuracy else uiState.gameStats.white.accuracy
            } else null,
            pieceSet = uiState.settings.pieceSet
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Board + EvalBar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ((1f - boardMaxHeightFraction) * 32).dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.settings.showEvalBar) {
                    ChessEvalBarComposable(
                        evaluation = uiState.currentEvaluation,
                        isFlipped = uiState.isFlipped,
                        modifier = Modifier
                            .height(320.dp)
                            .padding(end = 6.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ChessBoardComposable(
                        boardState = uiState.boardState,
                        selectedSquare = uiState.selectedSquare,
                        legalMoves = uiState.legalMoves,
                        lastMoveFrom = currentMoveNode?.from,
                        lastMoveTo = currentMoveNode?.to,
                        lastMoveCategory = currentMoveAnalysis?.category,
                        topLines = uiState.currentTopLines,
                        bestMoveUci = currentMoveAnalysis?.bestMoveUci
                            ?: uiState.currentTopLines.firstOrNull()?.bestMoveUci,
                        userArrows = uiState.userArrows,
                        squareHighlights = uiState.squareHighlights,
                        pendingPromotionMove = uiState.pendingPromotionMove,
                        isFlipped = uiState.isFlipped,
                        settings = uiState.settings,
                        onSquareClick = onSquareClick,
                        onPieceDraggedMove = onPieceDraggedMove,
                        onSelectPromotion = onSelectPromotion,
                        onCancelPromotion = onCancelPromotion
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Player Bar (Active user)
        PlayerMaterialBar(
            name = if (topPlayerIsBlack) uiState.headers.white else uiState.headers.black,
            elo = if (topPlayerIsBlack) uiState.headers.whiteElo else uiState.headers.blackElo,
            isBlack = !topPlayerIsBlack,
            capturedPieces = if (topPlayerIsBlack) capturedInfo.capturedByWhite else capturedInfo.capturedByBlack,
            materialAdvantage = if (topPlayerIsBlack) capturedInfo.whiteAdvantage else capturedInfo.blackAdvantage,
            accuracy = if (uiState.analyses.isNotEmpty()) {
                if (topPlayerIsBlack) uiState.gameStats.white.accuracy else uiState.gameStats.black.accuracy
            } else null,
            pieceSet = uiState.settings.pieceSet
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Navigation, Annotation & Stockfish Analysis Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgSecondary)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onJumpToStart, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FastRewind, contentDescription = "В начало", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onStepBackward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Назад", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onStepForward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Вперёд", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onJumpToEnd, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FastForward, contentDescription = "В конец", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onFlipBoard, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Перевернуть", tint = AccentTeal, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onToggleAnnotationMode,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (uiState.annotationMode) AccentGold.copy(alpha = 0.22f) else Color.Transparent)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Рисовать стрелки",
                        tint = if (uiState.annotationMode) AccentGold else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (uiState.userArrows.isNotEmpty() || uiState.squareHighlights.isNotEmpty()) {
                    IconButton(onClick = onClearAnnotations, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Очистить стрелки",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(onClick = onSaveGame, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Сохранить в SQLite", tint = AccentGold, modifier = Modifier.size(17.dp))
                }
            }

            if (uiState.isAnalyzing) {
                Button(
                    onClick = onCancelAnalysis,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCA3431)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(text = "Стоп", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onRunAnalysis,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Анализ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (uiState.isAnalyzing) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                LinearProgressIndicator(
                    progress = { uiState.analysisProgress },
                    color = AccentTeal,
                    trackColor = BgCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Text(
                    text = uiState.analysisPlyText,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Current Move Classification & MultiPV Engine Lines Box
        MoveClassificationAndMultiPvCard(
            moveAnalysis = currentMoveAnalysis,
            topLines = uiState.currentTopLines,
            engineDepth = uiState.settings.engineDepth
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Tabs: 0 = Ходы, 1 = Статистика (11 категорий), 2 = График оценки
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = BgSecondary,
            contentColor = AccentGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = AccentGreen
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            listOf("Ходы (${uiState.moveNodes.size})", "Статистика", "График").forEachIndexed { idx, title ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (selectedSubTab == idx) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedSubTab == idx) TextPrimary else TextSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(BgSecondary, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .padding(6.dp)
        ) {
            when (selectedSubTab) {
                0 -> MoveListTableComposable(
                    nodes = uiState.moveNodes,
                    analyses = uiState.analyses,
                    currentPly = uiState.currentPly,
                    onSelectPly = onJumpToPly
                )
                1 -> FullGameStatsPanelComposable(
                    stats = uiState.gameStats,
                    openingSummary = uiState.openingSummary
                )
                2 -> InteractiveEvaluationGraphComposable(
                    analyses = uiState.analyses,
                    currentPly = uiState.currentPly,
                    onSelectPly = onJumpToPly
                )
            }
        }
    }
}

@Composable
private fun OpeningSummaryStrip(
    openingSummary: OpeningAnalysisSummary,
    result: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgSecondary)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = AccentGold.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = openingSummary.eco,
                    color = AccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (openingSummary.variation != null) {
                    "${openingSummary.name}: ${openingSummary.variation}"
                } else {
                    openingSummary.name
                },
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = result,
            color = AccentGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayerMaterialBar(
    name: String,
    elo: String,
    isBlack: Boolean,
    capturedPieces: List<ChessPiece>,
    materialAdvantage: Int,
    accuracy: Double?,
    pieceSet: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgSecondary)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isBlack) Color(0xFF1F2937) else Color(0xFFF3F4F6))
                    .border(1.dp, Color(0xFF6B7280), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (elo.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($elo)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Captured pieces row rendered with actual piece PNGs/SVGs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-3).dp)
            ) {
                capturedPieces.take(10).forEach { capPiece ->
                    ChessPieceImageComposable(
                        piece = capPiece,
                        pieceSet = pieceSet,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            if (materialAdvantage > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+$materialAdvantage",
                    color = AccentGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (accuracy != null && accuracy > 0.0) {
            Surface(
                color = AccentGreen.copy(alpha = 0.18f),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text(
                    text = "${"%.1f".format(java.util.Locale.US, accuracy)}%",
                    color = AccentGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MoveClassificationAndMultiPvCard(
    moveAnalysis: MoveAnalysis?,
    topLines: List<EngineLine>,
    engineDepth: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (moveAnalysis != null) {
            val cat = moveAnalysis.category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = cat.iconRes),
                        contentDescription = cat.labelRu,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${moveAnalysis.moveNumber}${if (moveAnalysis.color == PieceColor.WHITE) "." else "..."} ${moveAnalysis.san} — ${cat.labelRu}",
                                color = cat.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Точность: ${"%.1f".format(java.util.Locale.US, moveAnalysis.accuracy)}%",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = moveAnalysis.explanation,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Top MultiPV Engine Lines
        if (topLines.isNotEmpty()) {
            if (moveAnalysis != null) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (line in topLines.take(3)) {
                    val evalBadge = when {
                        line.mate != null -> "M${abs(line.mate)}"
                        else -> {
                            val p = (line.cp ?: 0) / 100.0
                            if (p >= 0) "+%.2f".format(java.util.Locale.US, p)
                            else "%.2f".format(java.util.Locale.US, p)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (line.multipv == 1) AccentGreen.copy(alpha = 0.22f) else BgElevated,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = evalBadge,
                                color = if (line.multipv == 1) AccentGreen else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = line.pvSan.ifEmpty { line.pvUci }.take(7).joinToString(" "),
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "d${line.depth.takeIf { it > 0 } ?: engineDepth}",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveListTableComposable(
    nodes: List<MoveTreeNode>,
    analyses: List<MoveAnalysis>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit
) {
    if (nodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Сделайте ход на доске или загрузите партию из архива / Chess.com",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        return
    }

    val pairs = remember(nodes) { nodes.chunked(2) }
    val listState = rememberLazyListState()

    LaunchedEffect(currentPly) {
        if (currentPly > 0) {
            val rowIdx = (currentPly - 1) / 2
            listState.animateScrollToItem(rowIdx.coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(pairs) { index, pair ->
            val whiteNode = pair.getOrNull(0)
            val blackNode = pair.getOrNull(1)
            val whiteAnalysis = whiteNode?.let { analyses.getOrNull(it.ply - 1) ?: it.analysis }
            val blackAnalysis = blackNode?.let { analyses.getOrNull(it.ply - 1) ?: it.analysis }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (index % 2 == 0) BgCard.copy(alpha = 0.5f) else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(28.dp)
                )

                if (whiteNode != null) {
                    MoveCellButton(
                        node = whiteNode,
                        analysis = whiteAnalysis,
                        isSelected = currentPly == whiteNode.ply,
                        onClick = { onSelectPly(whiteNode.ply) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (blackNode != null) {
                    MoveCellButton(
                        node = blackNode,
                        analysis = blackAnalysis,
                        isSelected = currentPly == blackNode.ply,
                        onClick = { onSelectPly(blackNode.ply) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoveCellButton(
    node: MoveTreeNode,
    analysis: MoveAnalysis?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (isSelected) AccentGreen.copy(alpha = 0.30f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (analysis != null) {
                Image(
                    painter = painterResource(id = analysis.category.iconRes),
                    contentDescription = analysis.category.labelRu,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = node.san,
                color = analysis?.category?.color ?: TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
            )
        }

        if (analysis != null) {
            val evalStr = when {
                analysis.evalAfter?.mate != null -> "M${abs(analysis.evalAfter.mate)}"
                else -> {
                    val p = (analysis.evalAfter?.cp ?: 0) / 100.0
                    if (p >= 0) "+%.1f".format(java.util.Locale.US, p)
                    else "%.1f".format(java.util.Locale.US, p)
                }
            }
            Text(
                text = evalStr,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Full Game Statistics Panel matching Web `MoveDetailsPanel.tsx`:
 * - White & Black Overall Accuracy (Lichess volatility-weighted + harmonic mean)
 * - Average Centipawn Loss (ACPL) & Estimated Performance ELO
 * - Opening / Middlegame / Endgame phase accuracy
 * - Opening Book deviation info
 * - Complete 11-row Move Category Breakdown with original PNG badges (`R.drawable.cat_*`)
 */
@Composable
private fun FullGameStatsPanelComposable(
    stats: GameStatistics,
    openingSummary: OpeningAnalysisSummary
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Accuracy & ELO Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerAccuracySummaryCard(
                label = "Белые",
                accuracy = stats.white.accuracy,
                acpl = stats.white.averageCentipawnLoss,
                elo = stats.white.estimatedElo,
                modifier = Modifier.weight(1f)
            )
            PlayerAccuracySummaryCard(
                label = "Чёрные",
                accuracy = stats.black.accuracy,
                acpl = stats.black.averageCentipawnLoss,
                elo = stats.black.estimatedElo,
                modifier = Modifier.weight(1f)
            )
        }

        // Opening Deviation Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Дебютная теория: ${openingSummary.eco} — ${openingSummary.name}",
                    color = AccentGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (openingSummary.variation != null) {
                    Text(
                        text = "Вариант: ${openingSummary.variation}",
                        color = TextPrimary,
                        fontSize = 10.sp
                    )
                }
                val dev = openingSummary.deviation
                if (dev != null) {
                    Text(
                        text = buildString {
                            append("Выход из книги на ходу ${dev.moveNumber}${if (dev.color == PieceColor.WHITE) "." else "..."} ${dev.playedSan}")
                            if (dev.bookMovesSan.isNotEmpty()) {
                                append(" (по теории: ${dev.bookMovesSan.joinToString(", ")})")
                            }
                        },
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                } else if (openingSummary.lastBookPly > 0) {
                    Text(
                        text = "Теоретических полуходов по книге: ${openingSummary.lastBookPly}",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Phase Accuracy Breakdown (Opening / Middlegame / Endgame)
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Точность по стадиям партии",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                PhaseAccuracyRow("Дебют (1–12 ход)", stats.openingAccuracyWhite, stats.openingAccuracyBlack)
                PhaseAccuracyRow("Миттельшпиль (13–28)", stats.middlegameAccuracyWhite, stats.middlegameAccuracyBlack)
                PhaseAccuracyRow("Эндшпиль (29+)", stats.endgameAccuracyWhite, stats.endgameAccuracyBlack)
            }
        }

        // Complete 11 Move Categories Table with original PNG icons
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Классификация ходов (все 11 категорий)",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                for (cat in MoveCategory.entries) {
                    val wCount = stats.white.categoryCounts[cat] ?: 0
                    val bCount = stats.black.categoryCounts[cat] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = wCount.toString(),
                            color = if (wCount > 0) cat.color else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = cat.iconRes),
                                contentDescription = cat.labelRu,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.labelRu,
                                color = cat.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = bCount.toString(),
                            color = if (bCount > 0) cat.color else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseAccuracyRow(label: String, whiteAcc: Double, blackAcc: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (whiteAcc > 0) "${"%.1f".format(java.util.Locale.US, whiteAcc)}%" else "—",
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp
        )
        Text(
            text = if (blackAcc > 0) "${"%.1f".format(java.util.Locale.US, blackAcc)}%" else "—",
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PlayerAccuracySummaryCard(
    label: String,
    accuracy: Double,
    acpl: Double,
    elo: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(
            text = "${"%.1f".format(java.util.Locale.US, accuracy)}%",
            color = AccentGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "ACPL: ${"%.1f".format(java.util.Locale.US, acpl)} • ~$elo ELO",
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun InteractiveEvaluationGraphComposable(
    analyses: List<MoveAnalysis>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit
) {
    if (analyses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Нажмите «Анализ», чтобы построить интерактивный график оценки Stockfish",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
            .pointerInput(analyses.size) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    val targetPly = (ratio * analyses.size).toInt().coerceIn(1, analyses.size)
                    onSelectPly(targetPly)
                }
            }
    ) {
        val midY = size.height / 2f
        drawLine(
            color = Color(0xFF4B5563),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.5f
        )

        val stepX = size.width / analyses.size.coerceAtLeast(1).toFloat()
        val linePath = Path()
        linePath.moveTo(0f, midY)

        for ((idx, item) in analyses.withIndex()) {
            val x = (idx + 1) * stepX
            val clampedCp = item.centipawnsAfter.coerceIn(-800, 800)
            val y = midY - (clampedCp / 800f) * (size.height * 0.45f)
            linePath.lineTo(x, y)
        }

        drawPath(
            path = linePath,
            color = Color(0xFF10B981),
            style = Stroke(width = 3f)
        )

        // Highlight blunders and mistakes with dots on the curve
        for ((idx, item) in analyses.withIndex()) {
            if (item.category == MoveCategory.BLUNDER ||
                item.category == MoveCategory.MISTAKE ||
                item.category == MoveCategory.BRILLIANT
            ) {
                val x = (idx + 1) * stepX
                val clampedCp = item.centipawnsAfter.coerceIn(-800, 800)
                val y = midY - (clampedCp / 800f) * (size.height * 0.45f)
                drawCircle(
                    color = item.category.color,
                    radius = 4.5f,
                    center = Offset(x, y)
                )
            }
        }

        if (currentPly in 1..analyses.size) {
            val curX = currentPly * stepX
            drawLine(
                color = Color(0xFFF59E0B),
                start = Offset(curX, 0f),
                end = Offset(curX, size.height),
                strokeWidth = 2.5f
            )
        }
    }
}
