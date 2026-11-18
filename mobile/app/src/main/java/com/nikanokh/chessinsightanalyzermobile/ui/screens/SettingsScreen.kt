package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.model.AppSettings
import com.nikanokh.chessinsightanalyzermobile.ui.theme.*
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessUiState

@Composable
fun SettingsScreen(
    uiState: ChessUiState,
    onUpdateSettings: (AppSettings) -> Unit,
    onResetSettings: () -> Unit
) {
    val s = uiState.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBackground)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ChessSurface)
                .border(1.dp, ChessBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Настройки доски и анализа (Jetpack Compose)",
                color = ChessTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            SettingToggleRow(
                label = "Показывать шкалу оценки слева от доски",
                checked = s.showEvalBar,
                onCheckedChange = { onUpdateSettings(s.copy(showEvalBar = it)) }
            )

            SettingToggleRow(
                label = "Подсветка доступных ходов фигур",
                checked = s.highlightLegalMoves,
                onCheckedChange = { onUpdateSettings(s.copy(highlightLegalMoves = it)) }
            )

            SettingToggleRow(
                label = "Стрелки лучших ходов движка на доске",
                checked = s.showEngineArrows,
                onCheckedChange = { onUpdateSettings(s.copy(showEngineArrows = it)) }
            )

            SettingToggleRow(
                label = "Автопрокрутка списка ходов при ходе на доске",
                checked = s.autoScrollMoves,
                onCheckedChange = { onUpdateSettings(s.copy(autoScrollMoves = it)) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ChessSurface)
                .border(1.dp, ChessBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Глубина тактического анализа",
                color = ChessTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 14, 18, 22).forEach { depth ->
                    val active = s.engineDepth == depth
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) ChessGreenPrimary else ChessSurfaceElevated)
                            .border(
                                1.dp,
                                if (active) ChessGreenPrimary else ChessBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onUpdateSettings(s.copy(engineDepth = depth)) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Depth $depth",
                            color = if (active) Color.White else ChessTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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
                text = "О нативном приложении Android",
                color = ChessTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Стек: 100% Kotlin 2.0 + Jetpack Compose Material 3 + Coroutines",
                color = ChessTextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Application ID: com.nikanokh.chessinsightanalyzermobile",
                color = ChessTextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Локальная БД: SQLiteOpenHelper (chess_insight_analyzer_v4.db) · Партий: ${uiState.savedGames.size}",
                color = ChessTextSecondary,
                fontSize = 12.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ChessDanger.copy(alpha = 0.16f))
                    .clickable(onClick = onResetSettings)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Сбросить настройки по умолчанию",
                    color = ChessDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ChessTextPrimary,
            fontSize = 13.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ChessGreenPrimary
            )
        )
    }
}
