package com.nikanokh.chessinsightanalyzermobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikanokh.chessinsightanalyzermobile.data.remote.ChessComGameItem
import com.nikanokh.chessinsightanalyzermobile.data.remote.OnlineImportService
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

/**
 * Games Archive & Online Import Screen:
 * Tab 0: Архив SQLite (сохранённые партии, поиск, избранное)
 * Tab 1: Chess.com PubAPI (полная синхронизация с Web `ChessComModal.tsx`: профиль, все месячные архивы, пагинация, фильтры по результату/цвету/контролю)
 * Tab 2: PGN / FEN / Lichess (ручной ввод PGN, FEN и пакетный импорт с Lichess)
 */
@Composable
fun GamesArchiveScreen(
    uiState: ChessUiState,
    onOpenGame: (SavedGameRecord) -> Unit,
    onToggleFavorite: (SavedGameRecord) -> Unit,
    onDeleteGame: (String) -> Unit,
    onImportPgn: (String) -> Unit,
    onImportFen: (String) -> Unit,
    onImportOnline: (String, String) -> Unit,
    onSearchChessComPlayer: (String) -> Unit,
    onLoadMoreChessComArchives: () -> Unit,
    onLoadAllChessComArchives: () -> Unit,
    onSelectChessComArchiveUrl: (String) -> Unit,
    onUpdateChessComFilters: (String, String, String) -> Unit,
    onSelectChessComGame: (ChessComGameItem) -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = BgSecondary,
            contentColor = AccentGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = AccentGreen
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            val tabs = listOf(
                "Архив (${uiState.savedGames.size})",
                "Chess.com API",
                "PGN / FEN / Lichess"
            )
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeSubTab == idx,
                    onClick = { activeSubTab = idx },
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (activeSubTab == idx) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSubTab == idx) TextPrimary else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (activeSubTab) {
            0 -> LocalSqliteArchiveTab(
                savedGames = uiState.savedGames,
                onOpenGame = onOpenGame,
                onToggleFavorite = onToggleFavorite,
                onDeleteGame = onDeleteGame
            )
            1 -> ChessComBrowserTab(
                uiState = uiState,
                onSearchPlayer = onSearchChessComPlayer,
                onLoadMoreArchives = onLoadMoreChessComArchives,
                onLoadAllArchives = onLoadAllChessComArchives,
                onSelectArchiveUrl = onSelectChessComArchiveUrl,
                onUpdateFilters = onUpdateChessComFilters,
                onSelectGame = onSelectChessComGame
            )
            2 -> PgnFenLichessImportTab(
                uiState = uiState,
                onImportPgn = onImportPgn,
                onImportFen = onImportFen,
                onImportOnline = onImportOnline
            )
        }
    }
}

@Composable
private fun LocalSqliteArchiveTab(
    savedGames: List<SavedGameRecord>,
    onOpenGame: (SavedGameRecord) -> Unit,
    onToggleFavorite: (SavedGameRecord) -> Unit,
    onDeleteGame: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }

    val filteredGames = remember(savedGames, searchQuery, onlyFavorites) {
        savedGames.filter { game ->
            val matchesFav = !onlyFavorites || game.isFavorite
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                game.title.lowercase().contains(q) ||
                game.white.lowercase().contains(q) ||
                game.black.lowercase().contains(q) ||
                game.openingName.lowercase().contains(q) ||
                game.eco.lowercase().contains(q)
            matchesFav && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по игроку, ECO или дебюту...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = BgCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = onlyFavorites,
                onClick = { onlyFavorites = !onlyFavorites },
                label = { Text("★", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGold.copy(alpha = 0.25f),
                    selectedLabelColor = AccentGold
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredGames.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Партии не найдены", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BgSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGame(game) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = AccentGold.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = game.eco,
                                            color = AccentGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = game.openingName,
                                        color = AccentTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${game.white} (${game.whiteElo}) vs ${game.black} (${game.blackElo})",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${game.event} • ${game.date} • Результат: ${game.result} • ${game.movesCount} полуходов",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                if (game.whiteAccuracy != null && game.blackAccuracy != null) {
                                    Text(
                                        text = "Точность: Белые ${"%.1f".format(java.util.Locale.US, game.whiteAccuracy)}% | Чёрные ${"%.1f".format(java.util.Locale.US, game.blackAccuracy)}%",
                                        color = AccentGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onToggleFavorite(game) }) {
                                    Icon(
                                        imageVector = if (game.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Избранное",
                                        tint = if (game.isFavorite) AccentGold else TextSecondary
                                    )
                                }
                                IconButton(onClick = { onDeleteGame(game.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = Color(0xFFCA3431)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1:1 port of Web `src/components/Modals/ChessComModal.tsx`:
 * - Search by Chess.com username + Quick Grandmaster Presets (`Hikaru`, `MagnusCarlsen`, `DanielNaroditsky`, `GothamChess`)
 * - Player Profile header (username, title badge, followers, archive counters)
 * - Monthly Archive Selector + "Подгрузить ещё 12 мес." + "Загрузить все архивы"
 * - Filters by Result (`all`/`win`/`draw`/`loss`), Color (`all`/`w`/`b`), and Time Class (`all`/`bullet`/`blitz`/`rapid`/`daily`)
 * - Deduplicated list of all player games with one-tap "Анализировать"
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChessComBrowserTab(
    uiState: ChessUiState,
    onSearchPlayer: (String) -> Unit,
    onLoadMoreArchives: () -> Unit,
    onLoadAllArchives: () -> Unit,
    onSelectArchiveUrl: (String) -> Unit,
    onUpdateFilters: (String, String, String) -> Unit,
    onSelectGame: (ChessComGameItem) -> Unit
) {
    var usernameInput by remember(uiState.chessComUsername) {
        mutableStateOf(uiState.chessComUsername.ifBlank { "Hikaru" })
    }

    val targetUserLower = (uiState.chessComProfile?.username ?: usernameInput).trim().lowercase()

    val filteredGames = remember(
        uiState.chessComGames,
        uiState.chessComResultFilter,
        uiState.chessComColorFilter,
        uiState.chessComTimeClassFilter,
        targetUserLower
    ) {
        uiState.chessComGames.filter { g ->
            val isWhite = g.white.username.lowercase() == targetUserLower
            val playerInfo = if (isWhite) g.white else g.black
            val outcome = OnlineImportService.mapChessComResult(playerInfo.result).outcome

            if (uiState.chessComColorFilter == "w" && !isWhite) return@filter false
            if (uiState.chessComColorFilter == "b" && isWhite) return@filter false
            if (uiState.chessComResultFilter != "all" && outcome != uiState.chessComResultFilter) return@filter false
            if (uiState.chessComTimeClassFilter != "all" && g.timeClass != uiState.chessComTimeClassFilter) return@filter false
            true
        }
    }

    val hasMoreArchivesToLoad = uiState.chessComSelectedArchiveUrl == "all_loaded" &&
        uiState.chessComLoadedArchiveCount < uiState.chessComArchiveUrls.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input + Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                placeholder = { Text("Никнейм на Chess.com (напр., Hikaru)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = BgCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onSearchPlayer(usernameInput) },
                enabled = !uiState.chessComLoading && usernameInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                if (uiState.chessComLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text("Найти", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Grandmaster Presets
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(text = "Быстрый выбор:", color = TextSecondary, fontSize = 11.sp)
            for (preset in OnlineImportService.QUICK_GM_PRESETS) {
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        usernameInput = preset
                        onSearchPlayer(preset)
                    }
                ) {
                    Text(
                        text = preset,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Error Banner
        if (uiState.chessComError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1A1A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.chessComError,
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Player Profile + Archive Pagination Controls + Filters
        val profile = uiState.chessComProfile
        if (profile != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.username.firstOrNull()?.uppercase() ?: "?",
                                    color = AccentGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!profile.title.isNullOrBlank()) {
                                        Surface(
                                            color = Color(0xFFCA3431),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = profile.title,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = profile.username,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Архивов: ${uiState.chessComLoadedArchiveCount}/${uiState.chessComArchiveUrls.size} мес. • Найдено: ${filteredGames.size} из ${uiState.chessComGames.size} партий",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (hasMoreArchivesToLoad) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = onLoadMoreArchives,
                                    enabled = !uiState.chessComLoadingMore,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (uiState.chessComLoadingMore) "Загрузка..." else "+12 мес.",
                                        fontSize = 10.sp,
                                        color = AccentGreen
                                    )
                                }
                                OutlinedButton(
                                    onClick = onLoadAllArchives,
                                    enabled = !uiState.chessComLoadingMore,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Все (${uiState.chessComArchiveUrls.size})",
                                        fontSize = 10.sp,
                                        color = AccentTeal
                                    )
                                }
                            }
                        }
                    }

                    // Monthly Archive Horizontal Selector (All Loaded vs Specific Month)
                    if (uiState.chessComArchiveUrls.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            val allSelected = uiState.chessComSelectedArchiveUrl == "all_loaded"
                            FilterChip(
                                selected = allSelected,
                                onClick = { onSelectArchiveUrl("all_loaded") },
                                label = {
                                    Text(
                                        "Все загруженные (${uiState.chessComLoadedArchiveCount} мес.)",
                                        fontSize = 10.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreen.copy(alpha = 0.25f),
                                    selectedLabelColor = AccentGreen
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                            for (archUrl in uiState.chessComArchiveUrls.take(24)) {
                                val sel = uiState.chessComSelectedArchiveUrl == archUrl
                                FilterChip(
                                    selected = sel,
                                    onClick = { onSelectArchiveUrl(archUrl) },
                                    label = {
                                        Text(
                                            OnlineImportService.formatArchiveMonthLabel(archUrl),
                                            fontSize = 10.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                                        selectedLabelColor = AccentTeal
                                    ),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }

                    // Filter Chips Row (Outcome + Color + Time Class)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        val resultOptions = listOf(
                            "all" to "Все исходы",
                            "win" to "Победы",
                            "draw" to "Ничьи",
                            "loss" to "Поражения"
                        )
                        for ((code, lbl) in resultOptions) {
                            FilterChip(
                                selected = uiState.chessComResultFilter == code,
                                onClick = {
                                    onUpdateFilters(
                                        code,
                                        uiState.chessComColorFilter,
                                        uiState.chessComTimeClassFilter
                                    )
                                },
                                label = { Text(lbl, fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }

                        val colorOptions = listOf(
                            "all" to "Любой цвет",
                            "w" to "Белые",
                            "b" to "Чёрные"
                        )
                        for ((code, lbl) in colorOptions) {
                            FilterChip(
                                selected = uiState.chessComColorFilter == code,
                                onClick = {
                                    onUpdateFilters(
                                        uiState.chessComResultFilter,
                                        code,
                                        uiState.chessComTimeClassFilter
                                    )
                                },
                                label = { Text(lbl, fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }

                        val timeOptions = listOf(
                            "all" to "Все контроли",
                            "bullet" to "Bullet",
                            "blitz" to "Blitz",
                            "rapid" to "Rapid",
                            "daily" to "Daily"
                        )
                        for ((code, lbl) in timeOptions) {
                            FilterChip(
                                selected = uiState.chessComTimeClassFilter == code,
                                onClick = {
                                    onUpdateFilters(
                                        uiState.chessComResultFilter,
                                        uiState.chessComColorFilter,
                                        code
                                    )
                                },
                                label = { Text(lbl, fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Chess.com Games List
        if (filteredGames.isEmpty() && !uiState.chessComLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.chessComProfile == null) {
                        "Введите никнейм игрока Chess.com или выберите гроссмейстера выше"
                    } else {
                        "Нет партий, подходящих под выбранные фильтры"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = filteredGames,
                    key = { OnlineImportService.getChessComGameKey(it) }
                ) { game ->
                    val isPlayerWhite = game.white.username.lowercase() == targetUserLower
                    val playerInfo = if (isPlayerWhite) game.white else game.black
                    val outcomeInfo = OnlineImportService.mapChessComResult(playerInfo.result)
                    val badgeColor = when (outcomeInfo.outcome) {
                        "win" -> Color(0xFF10B981)
                        "draw" -> Color(0xFF9CA3AF)
                        else -> Color(0xFFEF4444)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = BgSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGame(game) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = outcomeInfo.labelRu,
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${game.timeClass.uppercase()} (${game.timeControl})",
                                        color = AccentGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = OnlineImportService.formatEndTime(game.endTime),
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⬜ ${game.white.username} (${game.white.rating})",
                                    color = if (isPlayerWhite) AccentGreen else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isPlayerWhite) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                                Text(
                                    text = "⬛ ${game.black.username} (${game.black.rating})",
                                    color = if (!isPlayerWhite) AccentGreen else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (!isPlayerWhite) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { onSelectGame(game) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Анализ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PgnFenLichessImportTab(
    uiState: ChessUiState,
    onImportPgn: (String) -> Unit,
    onImportFen: (String) -> Unit,
    onImportOnline: (String, String) -> Unit
) {
    var pgnText by remember { mutableStateOf("") }
    var fenText by remember { mutableStateOf("") }
    var lichessUser by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Lichess Import Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Пакетный импорт партий с Lichess.org",
                    color = AccentTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lichessUser,
                        onValueChange = { lichessUser = it },
                        placeholder = { Text("Никнейм на Lichess (напр., DrNykterstein)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onImportOnline("Lichess", lichessUser) },
                        enabled = !uiState.isImportingOnline && lichessUser.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Загрузить", fontSize = 12.sp)
                    }
                }
                if (uiState.onlineImportMessage != null) {
                    Text(
                        text = uiState.onlineImportMessage,
                        color = AccentGold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // PGN Paste Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Загрузить партию из текста PGN",
                    color = AccentGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = pgnText,
                    onValueChange = { pgnText = it },
                    placeholder = { Text("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6...", fontSize = 12.sp) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (pgnText.isNotBlank()) {
                            onImportPgn(pgnText)
                            pgnText = ""
                        }
                    },
                    enabled = pgnText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Открыть PGN на доске и запустить анализ", fontWeight = FontWeight.Bold)
                }
            }
        }

        // FEN Position Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Установить позицию по строке FEN",
                    color = AccentGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = fenText,
                    onValueChange = { fenText = it },
                    placeholder = { Text("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (fenText.isNotBlank()) {
                            onImportFen(fenText)
                            fenText = ""
                        }
                    },
                    enabled = fenText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Установить позицию FEN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
