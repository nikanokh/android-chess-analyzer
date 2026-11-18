package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.chess.EcoOpeningBook
import com.nikanokh.chessinsightanalyzermobile.domain.model.SavedGameRecord
import com.nikanokh.chessinsightanalyzermobile.ui.components.EcoOpeningBadgeRow
import com.nikanokh.chessinsightanalyzermobile.ui.theme.*
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.AppTab
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessUiState

@Composable
fun HomeScreen(
    uiState: ChessUiState,
    onSelectTab: (AppTab) -> Unit,
    onStartNewGame: () -> Unit,
    onRunAnalysis: () -> Unit,
    onLoadSavedGame: (SavedGameRecord) -> Unit,
    onLoadSamplePgn: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBackground)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Active Game Hero Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ChessSurface)
                .border(1.dp, ChessBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ТЕКУЩАЯ ПАРТИЯ НА ДОСКЕ",
                    color = ChessGreenPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.metadata.platform} · ${uiState.metadata.date}",
                    color = ChessTextSecondary,
                    fontSize = 11.sp
                )
            }

            Text(
                text = buildAnnotatedString {
                    append("${uiState.metadata.whiteName} vs ${uiState.metadata.blackName} ")
                    withStyle(SpanStyle(color = ChessGreenPrimary, fontWeight = FontWeight.Bold)) {
                        append("(${uiState.metadata.result})")
                    }
                },
                color = ChessTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )

            EcoOpeningBadgeRow(opening = uiState.opening)

            uiState.statistics?.let { stats ->
                Text(
                    text = "Точность: Белые ${stats.whiteAccuracy}% (${stats.whiteAvgCpl} сП) · Чёрные ${stats.blackAccuracy}% (${stats.blackAvgCpl} сП)",
                    color = ChessGreenPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSelectTab(AppTab.BOARD) },
                    colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Открыть доску", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        onSelectTab(AppTab.BOARD)
                        onRunAnalysis()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (uiState.isAnalyzing) {
                            "Анализ ${uiState.analysisCurrent}/${uiState.analysisTotal}"
                        } else {
                            "Полный анализ"
                        },
                        color = ChessTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 2. KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(14.dp))
                    .clickable { onSelectTab(AppTab.GAMES) }
                    .padding(14.dp)
            ) {
                Text(
                    text = "${uiState.savedGames.size}",
                    color = ChessTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Партий в SQLite",
                    color = ChessTextSecondary,
                    fontSize = 12.sp
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(14.dp))
                    .clickable { onSelectTab(AppTab.REPERTOIRE) }
                    .padding(14.dp)
            ) {
                Text(
                    text = "${uiState.repertoireOpenings.size}",
                    color = ChessTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Дебютов ECO",
                    color = ChessTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // 3. Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onStartNewGame,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Новая партия", color = ChessTextPrimary, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { onSelectTab(AppTab.GAMES) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Импорт PGN / Архив", color = ChessTextPrimary, fontSize = 13.sp)
            }
        }

        // 4. Recent SQLite Games
        Text(
            text = "Последние партии в базе SQLite",
            color = ChessTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        uiState.savedGames.take(5).forEach { rec ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(12.dp))
                    .clickable { onLoadSavedGame(rec) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("${rec.metadata.whiteName} vs ${rec.metadata.blackName} ")
                        withStyle(SpanStyle(color = ChessGreenPrimary, fontWeight = FontWeight.Bold)) {
                            append("(${rec.metadata.result})")
                        }
                    },
                    color = ChessTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )

                EcoOpeningBadgeRow(opening = rec.opening)

                Text(
                    text = buildString {
                        append("${rec.metadata.platform} · ${rec.metadata.date}")
                        rec.statistics?.let { s ->
                            append(" · Точность ${s.whiteAccuracy}% / ${s.blackAccuracy}%")
                        }
                    },
                    color = ChessTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // 5. Classic Master Games
        Text(
            text = "Классические партии гроссмейстеров",
            color = ChessTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        EcoOpeningBook.sampleMasterGames.forEach { sample ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(12.dp))
                    .clickable { onLoadSamplePgn(sample.pgn) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sample.title,
                    color = ChessTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )
                Text(
                    text = sample.subtitle,
                    color = ChessTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
