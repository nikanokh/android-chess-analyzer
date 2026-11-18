package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.R
import com.nikanokh.chessinsightanalyzermobile.data.remote.OnlineImportService
import com.nikanokh.chessinsightanalyzermobile.domain.model.MoveCategory
import com.nikanokh.chessinsightanalyzermobile.domain.model.SavedGameRecord
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGold
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGreen
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentTeal
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgCard
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessUiState

@Composable
fun HomeScreen(
    uiState: ChessUiState,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToRepertoire: () -> Unit,
    onLoadSamplePgn: (String) -> Unit,
    onOpenSavedGame: (SavedGameRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Banner with Original Piece & Classification Icons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF283618), Color(0xFF1B2414), BgSecondary)
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = AccentGreen.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "STOCKFISH 16 UCI • 11 КАТЕГОРИЙ • ECO",
                            color = AccentGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.piece_wk),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(26.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.piece_wq),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(26.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.cat_brilliant),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Chess Insight Analyzer",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Полный шахматный аналитический комплекс: движок Stockfish UCI (MultiPV, Depth, Hash), точная формула Lichess Win% и гармонической точности, классификация всех 11 типов ходов с расчётом жертв SEE и загрузка всех архивов Chess.com.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onNavigateToAnalyzer,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Открыть доску", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onNavigateToArchive,
                        colors = ButtonDefaults.buttonColors(containerColor = BgCard)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Chess.com / Архив", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Summary KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiStatCard(
                title = "Партий в SQLite",
                value = uiState.savedGames.size.toString(),
                subtitle = "Локальная БД",
                color = AccentGreen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToArchive
            )
            KpiStatCard(
                title = "Линий репертуара",
                value = uiState.repertoire.size.toString(),
                subtitle = "Дебюты ECO",
                color = AccentTeal,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToRepertoire
            )
            KpiStatCard(
                title = "Глубина Stockfish",
                value = "d${uiState.settings.engineDepth}",
                subtitle = "MultiPV ${uiState.settings.multiPv}",
                color = AccentGold,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToAnalyzer
            )
        }

        // All 11 Move Categories Showcase Strip
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Все 11 категорий оценки ходов",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (cat in MoveCategory.entries) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = cat.iconRes),
                                contentDescription = cat.labelRu,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = cat.symbol,
                                color = cat.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Famous Masterpiece Games
        Text(
            text = "Исторические шедевры для мгновенного разбора",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        for (sample in OnlineImportService.SAMPLE_GAMES) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLoadSamplePgn(sample.pgn)
                        onNavigateToAnalyzer()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AccentGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = sample.eco,
                                    color = AccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sample.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sample.subtitle,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = "Анализировать",
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, color = TextSecondary, fontSize = 10.sp)
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(text = subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
