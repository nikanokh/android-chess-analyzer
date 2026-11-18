package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.nikanokh.chessinsightanalyzermobile.domain.engine.ChessAnalyzerEngine
import com.nikanokh.chessinsightanalyzermobile.domain.model.AppSettings
import com.nikanokh.chessinsightanalyzermobile.domain.model.BOARD_THEME_PRESETS
import com.nikanokh.chessinsightanalyzermobile.domain.model.ChessPiece
import com.nikanokh.chessinsightanalyzermobile.domain.model.ClassificationThresholds
import com.nikanokh.chessinsightanalyzermobile.domain.model.EngineConfig
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceType
import com.nikanokh.chessinsightanalyzermobile.ui.components.ChessPieceImageComposable
import com.nikanokh.chessinsightanalyzermobile.ui.components.parseHexColor
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGold
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGreen
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentTeal
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgCard
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * 1:1 port of Web `src/components/Settings/SettingsModal.tsx`
 * Synchronizes all 4 sections:
 * 1. Board & Piece Appearance (4 board color presets, Neo PNG vs Classic SVG pieces, board size, 6 toggles)
 * 2. Arrows & Visual Hints (engine & user arrow toggles, primary/secondary/user colors, width 14..32px, opacity 40..100%)
 * 3. Stockfish 16 UCI Engine Config (Depth 1..30, MultiPV 1..5, Hash 16..512 MB, Threads 1..4, Movetime ms)
 * 4. Move Classification Thresholds (Best, Excellent, Good, Inaccuracy, Mistake CPL cutoffs with instant reclassification)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onApplyThemePreset: (String) -> Unit,
    onResetSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Настройки приложения и анализатора",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        // =====================================================================
        // SECTION 1: Оформление доски и фигур
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Оформление доски и фигур",
                    color = AccentGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Цветовая схема доски (4 пресета)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (preset in BOARD_THEME_PRESETS) {
                        val isSelected = settings.boardThemeId == preset.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreen.copy(alpha = 0.2f) else BgCard)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AccentGreen else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onApplyThemePreset(preset.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(preset.lightSquare)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(preset.darkSquare)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = preset.nameRu,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "Активна",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Piece Set Selector ("neo" vs "classic_svg") with live piece preview
                Text(
                    text = "Набор шахматных фигур",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val pieceSets = listOf(
                        "neo" to "Фигуры Neo (PNG)",
                        "classic_svg" to "Классический (SVG)"
                    )
                    for ((key, title) in pieceSets) {
                        val selected = settings.pieceSet == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) AccentGreen.copy(alpha = 0.2f) else BgCard)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) AccentGreen else Color(0xFF3F3D39),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onUpdateSettings(settings.copy(pieceSet = key)) }
                                .padding(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ChessPieceImageComposable(
                                    piece = ChessPiece(PieceType.KING, PieceColor.WHITE),
                                    pieceSet = key,
                                    modifier = Modifier.size(28.dp)
                                )
                                ChessPieceImageComposable(
                                    piece = ChessPiece(PieceType.QUEEN, PieceColor.WHITE),
                                    pieceSet = key,
                                    modifier = Modifier.size(28.dp)
                                )
                                ChessPieceImageComposable(
                                    piece = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK),
                                    pieceSet = key,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                color = if (selected) AccentGreen else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Board Size Selector
                Text(
                    text = "Размер шахматной доски",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val sizes = listOf(
                        "auto" to "Авто (Адаптивный)",
                        "small" to "Компактная",
                        "medium" to "Средняя",
                        "large" to "Крупная"
                    )
                    for ((code, label) in sizes) {
                        FilterChip(
                            selected = settings.boardSize == code,
                            onClick = { onUpdateSettings(settings.copy(boardSize = code)) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen.copy(alpha = 0.25f),
                                selectedLabelColor = AccentGreen
                            )
                        )
                    }
                }

                // 6 Visual Toggles
                SettingSwitchRow(
                    label = "Плавная анимация перемещения фигур",
                    checked = settings.enableAnimations,
                    onCheckedChange = { onUpdateSettings(settings.copy(enableAnimations = it)) }
                )
                SettingSwitchRow(
                    label = "Подсветка последнего сделанного хода",
                    checked = settings.highlightLastMove,
                    onCheckedChange = { onUpdateSettings(settings.copy(highlightLastMove = it)) }
                )
                SettingSwitchRow(
                    label = "Подсказки легальных ходов выбранной фигуры",
                    checked = settings.highlightLegalMoves,
                    onCheckedChange = { onUpdateSettings(settings.copy(highlightLegalMoves = it)) }
                )
                SettingSwitchRow(
                    label = "Координаты полей доски (A–H, 1–8)",
                    checked = settings.showCoordinates,
                    onCheckedChange = { onUpdateSettings(settings.copy(showCoordinates = it)) }
                )
                SettingSwitchRow(
                    label = "Вертикальная шкала оценки позиции (Eval Bar)",
                    checked = settings.showEvalBar,
                    onCheckedChange = { onUpdateSettings(settings.copy(showEvalBar = it)) }
                )
                SettingSwitchRow(
                    label = "Информационная панель дебюта и метаданных",
                    checked = settings.showLeftSidebar,
                    onCheckedChange = { onUpdateSettings(settings.copy(showLeftSidebar = it)) }
                )
            }
        }

        // =====================================================================
        // SECTION 2: Стрелки и визуальные подсказки
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. Стрелки и визуальные подсказки",
                    color = AccentTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                SettingSwitchRow(
                    label = "Стрелки лучших ходов движка (MultiPV)",
                    checked = settings.showEngineArrows,
                    onCheckedChange = { onUpdateSettings(settings.copy(showEngineArrows = it)) }
                )
                SettingSwitchRow(
                    label = "Пользовательские стрелки на доске",
                    checked = settings.showUserArrows,
                    onCheckedChange = { onUpdateSettings(settings.copy(showUserArrows = it)) }
                )

                ArrowColorPickerRow(
                    label = "Цвет главной стрелки движка (PV1)",
                    selectedHex = settings.engineArrowColorHex,
                    options = listOf("#10B981", "#22C55E", "#06B6D4", "#8B5CF6", "#F59E0B"),
                    onSelect = { onUpdateSettings(settings.copy(engineArrowColorHex = it)) }
                )
                ArrowColorPickerRow(
                    label = "Цвет альтернативных линий движка (PV2+)",
                    selectedHex = settings.secondaryEngineArrowColorHex,
                    options = listOf("#3B82F6", "#6366F1", "#64748B", "#EC4899", "#14B8A6"),
                    onSelect = { onUpdateSettings(settings.copy(secondaryEngineArrowColorHex = it)) }
                )
                ArrowColorPickerRow(
                    label = "Цвет пользовательских стрелок",
                    selectedHex = settings.userArrowColorHex,
                    options = listOf("#F59E0B", "#EF4444", "#3B82F6", "#10B981", "#A855F7"),
                    onSelect = { onUpdateSettings(settings.copy(userArrowColorHex = it)) }
                )

                // Arrow width slider (14..32 px)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Толщина стрелок", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            text = "${settings.arrowWidth} px",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.arrowWidth.toFloat(),
                        onValueChange = {
                            onUpdateSettings(settings.copy(arrowWidth = it.roundToInt().coerceIn(14, 32)))
                        },
                        valueRange = 14f..32f,
                        steps = 17,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )
                }

                // Arrow opacity slider (40%..100%)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Непрозрачность стрелок", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            text = "${(settings.arrowOpacity * 100).roundToInt()}%",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.arrowOpacity * 100f,
                        onValueChange = {
                            onUpdateSettings(settings.copy(arrowOpacity = (it / 100f).coerceIn(0.4f, 1.0f)))
                        },
                        valueRange = 40f..100f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )
                }
            }
        }

        // =====================================================================
        // SECTION 3: Параметры движка Stockfish 16 UCI
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "3. Параметры движка Stockfish 16 UCI",
                    color = AccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                val cfg = settings.engineConfig

                // Depth 1..30
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Глубина анализа (Depth)", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            text = "${cfg.depth} полуходов",
                            color = AccentGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = cfg.depth.toFloat(),
                        onValueChange = {
                            onUpdateSettings(
                                settings.copy(engineConfig = cfg.copy(depth = it.roundToInt().coerceIn(1, 30)))
                            )
                        },
                        valueRange = 1f..30f,
                        steps = 28,
                        colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                    )
                }

                // MultiPV 1..5
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Количество лучших линий (MultiPV)", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            text = "${cfg.multiPv}",
                            color = AccentGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = cfg.multiPv.toFloat(),
                        onValueChange = {
                            onUpdateSettings(
                                settings.copy(engineConfig = cfg.copy(multiPv = it.roundToInt().coerceIn(1, 5)))
                            )
                        },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                    )
                }

                // Hash table size (16, 32, 64, 128, 256, 512 MB)
                Text(
                    text = "Размер хеш-таблицы (Hash МБ)",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (mb in listOf(16, 32, 64, 128, 256, 512)) {
                        FilterChip(
                            selected = cfg.hashMb == mb,
                            onClick = {
                                onUpdateSettings(settings.copy(engineConfig = cfg.copy(hashMb = mb)))
                            },
                            label = { Text("$mb МБ", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold.copy(alpha = 0.25f),
                                selectedLabelColor = AccentGold
                            )
                        )
                    }
                }

                // Threads 1..4
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Потоки вычислений (Threads)", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            text = "${cfg.threads}",
                            color = AccentGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = cfg.threads.toFloat(),
                        onValueChange = {
                            onUpdateSettings(
                                settings.copy(engineConfig = cfg.copy(threads = it.roundToInt().coerceIn(1, 4)))
                            )
                        },
                        valueRange = 1f..4f,
                        steps = 2,
                        colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                    )
                }

                // Movetime ms (0 = use depth, up to 5000 ms)
                val movetimeVal = cfg.movetimeMs ?: 0
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Лимит времени на ход (Movetime)", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            text = if (movetimeVal <= 0) "По глубине (0 мс)" else "$movetimeVal мс",
                            color = AccentGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = movetimeVal.toFloat(),
                        onValueChange = {
                            val ms = (it / 250f).roundToInt() * 250
                            onUpdateSettings(
                                settings.copy(
                                    engineConfig = cfg.copy(movetimeMs = if (ms <= 0) null else ms)
                                )
                            )
                        },
                        valueRange = 0f..5000f,
                        steps = 19,
                        colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                    )
                }
            }
        }

        // =====================================================================
        // SECTION 4: Пороги классификации ходов (в сантипешках CPL)
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "4. Пороги классификации ходов (CPL)",
                            color = AccentGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Изменение порогов мгновенно пересчитывает категории и точность текущей партии.",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onUpdateSettings(settings.copy(thresholds = ChessAnalyzerEngine.DEFAULT_THRESHOLDS))
                        }
                    ) {
                        Text("Сброс", fontSize = 11.sp, color = AccentGreen)
                    }
                }

                val th = settings.thresholds

                ThresholdSliderRow(
                    label = "Лучший ход (Best ≤ CPL)",
                    value = th.bestMaxCpl,
                    range = 5..50,
                    color = Color(0xFF779954),
                    onValueChange = {
                        onUpdateSettings(settings.copy(thresholds = th.copy(bestMaxCpl = it)))
                    }
                )
                ThresholdSliderRow(
                    label = "Отличный ход (Excellent ≤ CPL)",
                    value = th.excellentMaxCpl,
                    range = 20..100,
                    color = Color(0xFF81B64C),
                    onValueChange = {
                        onUpdateSettings(settings.copy(thresholds = th.copy(excellentMaxCpl = it)))
                    }
                )
                ThresholdSliderRow(
                    label = "Хороший ход (Good ≤ CPL)",
                    value = th.goodMaxCpl,
                    range = 40..160,
                    color = Color(0xFF95B776),
                    onValueChange = {
                        onUpdateSettings(settings.copy(thresholds = th.copy(goodMaxCpl = it)))
                    }
                )
                ThresholdSliderRow(
                    label = "Неточность (Inaccuracy ≤ CPL)",
                    value = th.inaccuracyMaxCpl,
                    range = 80..260,
                    color = Color(0xFFF0C15C),
                    onValueChange = {
                        onUpdateSettings(settings.copy(thresholds = th.copy(inaccuracyMaxCpl = it)))
                    }
                )
                ThresholdSliderRow(
                    label = "Ошибка (Mistake ≤ CPL, выше — Зевок)",
                    value = th.mistakeMaxCpl,
                    range = 150..500,
                    color = Color(0xFFE6912C),
                    onValueChange = {
                        onUpdateSettings(settings.copy(thresholds = th.copy(mistakeMaxCpl = it)))
                    }
                )
            }
        }

        Button(
            onClick = onResetSettings,
            colors = ButtonDefaults.buttonColors(containerColor = BgCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Сбросить все настройки по умолчанию", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArrowColorPickerRow(
    label: String,
    selectedHex: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = TextPrimary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (hex in options) {
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                val col = parseHexColor(hex)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                )
            }
        }
    }
}

@Composable
private fun ThresholdSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    color: Color,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 12.sp)
            Text(
                text = "$value сП",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGreen
            )
        )
    }
}
