package com.nikanokh.chessinsightanalyzermobile.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.domain.model.SavedGameRecord
import com.nikanokh.chessinsightanalyzermobile.ui.components.EcoOpeningBadgeRow
import com.nikanokh.chessinsightanalyzermobile.ui.theme.*
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessUiState
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun GamesArchiveScreen(
    uiState: ChessUiState,
    onLoadSavedGame: (SavedGameRecord) -> Unit,
    onImportPgnText: (String) -> Unit,
    onImportOnline: (platform: String, username: String) -> Unit,
    onDeleteGame: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showPasteBox by remember { mutableStateOf(false) }
    var showOnlineBox by remember { mutableStateOf(false) }
    var pgnPasteText by remember { mutableStateOf("") }
    var onlineUsername by remember { mutableStateOf("") }
    var onlinePlatform by remember { mutableStateOf("Lichess") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = BufferedReader(InputStreamReader(stream)).readText()
                    if (content.isNotBlank()) {
                        onImportPgnText(content)
                    }
                }
            }
        }
    }

    val filteredGames = remember(uiState.savedGames, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            uiState.savedGames
        } else {
            uiState.savedGames.filter { g ->
                val hay = listOf(
                    g.metadata.whiteName,
                    g.metadata.blackName,
                    g.opening.eco,
                    g.opening.name,
                    g.opening.variation,
                    g.metadata.date
                ).joinToString(" ").lowercase()
                hay.contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Файл .PGN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    showPasteBox = !showPasteBox
                    if (showPasteBox) showOnlineBox = false
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Текст PGN", color = ChessTextPrimary, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    showOnlineBox = !showOnlineBox
                    if (showOnlineBox) showPasteBox = false
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Lichess / Chess", color = ChessTextPrimary, fontSize = 12.sp)
            }
        }

        if (showPasteBox) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pgnPasteText,
                    onValueChange = { pgnPasteText = it },
                    placeholder = { Text("Вставьте PGN (например 1. e4 e5 2. Nf3 d6...)", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp),
                    maxLines = 6
                )
                Button(
                    onClick = {
                        if (pgnPasteText.isNotBlank()) {
                            onImportPgnText(pgnPasteText)
                            pgnPasteText = ""
                            showPasteBox = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Загрузить на доску и сохранить в SQLite", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showOnlineBox) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessSurface)
                    .border(1.dp, ChessBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = onlinePlatform == "Lichess",
                        onClick = { onlinePlatform = "Lichess" },
                        label = { Text("Lichess.org") }
                    )
                    FilterChip(
                        selected = onlinePlatform == "Chess.com",
                        onClick = { onlinePlatform = "Chess.com" },
                        label = { Text("Chess.com") }
                    )
                }
                OutlinedTextField(
                    value = onlineUsername,
                    onValueChange = { onlineUsername = it },
                    placeholder = { Text("Никнейм игрока (например DrNykterstein)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onImportOnline(onlinePlatform, onlineUsername) },
                    enabled = !uiState.isImportingOnline,
                    colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.isImportingOnline) "Загрузка..." else "Импортировать партии ($onlinePlatform)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск по игрокам или коду дебюта ECO (C41, Морфи...)", fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredGames, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChessSurface)
                        .border(1.dp, ChessBorder, RoundedCornerShape(14.dp))
                        .clickable { onLoadSavedGame(item) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("⬜ ${item.metadata.whiteName} vs ⬛ ${item.metadata.blackName} ")
                            withStyle(SpanStyle(color = ChessGreenPrimary, fontWeight = FontWeight.Bold)) {
                                append("(${item.metadata.result})")
                            }
                        },
                        color = ChessTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )

                    EcoOpeningBadgeRow(opening = item.opening)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                append("${item.metadata.platform} · ${item.metadata.date}")
                                item.statistics?.let { s ->
                                    append(" · ${s.whiteAccuracy}% / ${s.blackAccuracy}%")
                                }
                            },
                            color = ChessTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChessSurfaceElevated)
                                    .clickable {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, item.metadata.fullTitle)
                                            putExtra(Intent.EXTRA_TEXT, item.pgn)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Поделиться PGN")
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("PGN", color = ChessTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChessDanger.copy(alpha = 0.16f))
                                    .clickable { onDeleteGame(item.id) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("Удалить", color = ChessDanger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
