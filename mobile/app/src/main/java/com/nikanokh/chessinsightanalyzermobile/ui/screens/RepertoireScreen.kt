package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.ui.theme.*
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessUiState

@Composable
fun RepertoireScreen(
    uiState: ChessUiState,
    onRefreshRepertoire: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Дебютный репертуар (ECO)",
                    color = ChessTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Партий в базе SQLite: ${uiState.savedGames.size} · Уникальных дебютов: ${uiState.repertoireOpenings.size}",
                    color = ChessTextSecondary,
                    fontSize = 12.sp
                )
                Button(
                    onClick = onRefreshRepertoire,
                    colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Обновить статистику по базе SQLite", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(uiState.repertoireOpenings, key = { it.key }) { op ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(12.dp))
                    .padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ChessGoldEco.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.border(
                            1.dp,
                            ChessGoldEco.copy(alpha = 0.4f),
                            RoundedCornerShape(5.dp)
                        )
                    ) {
                        Text(
                            text = op.eco,
                            color = ChessGoldEco,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = op.openingName,
                        color = ChessTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Партий: ${op.gamesCount} · Побед белых: ${op.winRate}% · Ничьих: ${op.drawRate}% · Побед чёрных: ${op.lossRate}%",
                    color = ChessTextSecondary,
                    fontSize = 12.sp
                )

                op.variations.forEach { v ->
                    Text(
                        text = "• ${v.variationName} (${v.gamesCount} парт., Win ${v.winRate}%)",
                        color = ChessTextPrimary.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
