package com.nikanokh.chessinsightanalyzermobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikanokh.chessinsightanalyzermobile.ui.screens.*
import com.nikanokh.chessinsightanalyzermobile.ui.theme.*
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.AppTab
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessInsightTheme {
                ChessAnalyzerRootApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessAnalyzerRootApp(viewModel: ChessViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ChessBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.activeTab) {
                            AppTab.HOME -> "Chess Insight Analyzer"
                            AppTab.BOARD -> "Интерактивная доска"
                            AppTab.GAMES -> "Архив партий SQLite"
                            AppTab.REPERTOIRE -> "Дебютный репертуар ECO"
                            AppTab.SETTINGS -> "Настройки"
                        },
                        color = ChessTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChessSurface,
                    titleContentColor = ChessTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ChessSurface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple(AppTab.HOME, "Главная", Icons.Default.Home),
                    Triple(AppTab.BOARD, "Доска", Icons.Default.GridOn),
                    Triple(AppTab.GAMES, "Партии", Icons.Default.FolderSpecial),
                    Triple(AppTab.REPERTOIRE, "Дебюты", Icons.Default.Analytics),
                    Triple(AppTab.SETTINGS, "Настройки", Icons.Default.Settings)
                )
                tabs.forEach { (tab, label, icon) ->
                    val selected = uiState.activeTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChessGreenPrimary,
                            selectedTextColor = ChessGreenPrimary,
                            unselectedIconColor = ChessTextSecondary,
                            unselectedTextColor = ChessTextSecondary,
                            indicatorColor = ChessGreenPrimary.copy(alpha = 0.16f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            uiState.statusMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChessGreenPrimary.copy(alpha = 0.2f))
                        .clickable { viewModel.clearStatusMessage() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        color = ChessTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "✕",
                        color = ChessTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState.activeTab) {
                    AppTab.HOME -> HomeScreen(
                        uiState = uiState,
                        onSelectTab = viewModel::selectTab,
                        onStartNewGame = viewModel::startNewGame,
                        onRunAnalysis = viewModel::runFullGameAnalysis,
                        onLoadSavedGame = viewModel::loadSavedGame,
                        onLoadSamplePgn = { pgn -> viewModel.loadPgnString(pgn, "PGN", true) }
                    )

                    AppTab.BOARD -> AnalyzerScreen(
                        uiState = uiState,
                        onSquareClick = viewModel::onSquareClicked,
                        onSelectPly = viewModel::selectPly,
                        onGoStart = viewModel::goToStart,
                        onGoPrev = viewModel::goToPrev,
                        onGoNext = viewModel::goToNext,
                        onGoEnd = viewModel::goToEnd,
                        onRunAnalysis = viewModel::runFullGameAnalysis,
                        onCancelAnalysis = viewModel::cancelAnalysis,
                        onFlipBoard = viewModel::flipBoard
                    )

                    AppTab.GAMES -> GamesArchiveScreen(
                        uiState = uiState,
                        onLoadSavedGame = viewModel::loadSavedGame,
                        onImportPgnText = { pgn -> viewModel.loadPgnString(pgn, "PGN", true) },
                        onImportOnline = viewModel::importOnlineGames,
                        onDeleteGame = viewModel::deleteSavedGame
                    )

                    AppTab.REPERTOIRE -> RepertoireScreen(
                        uiState = uiState,
                        onRefreshRepertoire = viewModel::rebuildRepertoire
                    )

                    AppTab.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        onUpdateSettings = viewModel::updateSettings,
                        onResetSettings = viewModel::resetSettings
                    )
                }
            }
        }
    }
}
