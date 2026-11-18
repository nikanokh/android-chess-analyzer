package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.chess.EcoOpeningBook
import com.nikanokh.chessinsightanalyzermobile.domain.model.PieceColor
import com.nikanokh.chessinsightanalyzermobile.domain.model.RepertoireEntry
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGold
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGreen
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentTeal
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextSecondary

@Composable
fun RepertoireScreen(
    repertoire: List<RepertoireEntry>,
    onStartPractice: (RepertoireEntry) -> Unit,
    onAddRepertoire: (String, PieceColor, String, String, String, String) -> Unit,
    onDeleteRepertoire: (String) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var eco by remember { mutableStateOf("C50") }
    var pgnMoves by remember { mutableStateOf("1. e4 e5 2. Nf3 Nc6 3. Bc4") }
    var notes by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Дебютный репертуар и книга ECO",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Встроенная база: ${EcoOpeningBook.INITIAL_ECO_ENTRIES.size} классических систем ECO",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = { showAddForm = !showAddForm },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = if (showAddForm) "Закрыть" else "Добавить", fontSize = 12.sp)
            }
        }

        if (showAddForm) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedColor == PieceColor.WHITE,
                            onClick = { selectedColor = PieceColor.WHITE },
                            label = { Text("За Белых") }
                        )
                        FilterChip(
                            selected = selectedColor == PieceColor.BLACK,
                            onClick = { selectedColor = PieceColor.BLACK },
                            label = { Text("За Чёрных") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = eco,
                            onValueChange = { eco = it },
                            label = { Text("Код ECO") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Название варианта") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = pgnMoves,
                        onValueChange = { pgnMoves = it },
                        label = { Text("Ходы PGN (например, 1. e4 c5 2. Nf3 d6)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Ключевые идеи и планы") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (pgnMoves.isNotBlank()) {
                                onAddRepertoire(title, selectedColor, eco, pgnMoves, notes, "Мои линии")
                                showAddForm = false
                                title = ""
                                notes = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Сохранить в SQLite", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(repertoire, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = AccentGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.eco,
                                        color = AccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (item.color == PieceColor.WHITE) "Белые" else "Чёрные",
                                    color = AccentTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onStartPractice(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Тренировать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { onDeleteRepertoire(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = Color(0xFFCA3431)
                                    )
                                }
                            }
                        }

                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = item.pgnMoves,
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                        if (item.notes.isNotBlank()) {
                            Text(
                                text = item.notes,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Text(
                            text = "Тренировок: ${item.practicedCount} (успешно: ${item.correctCount})",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
