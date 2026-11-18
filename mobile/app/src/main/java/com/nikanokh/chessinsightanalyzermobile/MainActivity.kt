package com.nikanokh.chessinsightanalyzermobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikanokh.chessinsightanalyzermobile.ui.screens.AnalyzerScreen
import com.nikanokh.chessinsightanalyzermobile.ui.screens.GamesArchiveScreen
import com.nikanokh.chessinsightanalyzermobile.ui.screens.HomeScreen
import com.nikanokh.chessinsightanalyzermobile.ui.screens.RepertoireScreen
import com.nikanokh.chessinsightanalyzermobile.ui.screens.SettingsScreen
import com.nikanokh.chessinsightanalyzermobile.ui.theme.AccentGreen
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.BgSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.ChessInsightTheme
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextPrimary
import com.nikanokh.chessinsightanalyzermobile.ui.theme.TextSecondary
import com.nikanokh.chessinsightanalyzermobile.ui.viewmodel.ChessViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessInsightTheme {
                ChessAnalyzerMobileApp()
            }
        }
    }
}

private data class BottomNavTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ChessAnalyzerMobileApp(
    chessViewModel: ChessViewModel = viewModel()
) {
    val uiState by chessViewModel.uiState.collectAsState()
    var currentTab by rememberSaveable { mutableIntStateOf(1) } // Open directly on Analyzer Board

    val tabs = listOf(
        BottomNavTab("Главная", Icons.Default.Home),
        BottomNavTab("Анализатор", Icons.Default.Analytics),
        BottomNavTab("Партии", Icons.Default.FolderSpecial),
        BottomNavTab("Дебюты", Icons.Default.MenuBook),
        BottomNavTab("Настройки", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = BgPrimary,
        bottomBar = {
            NavigationBar(
                containerColor = BgSecondary,
                contentColor = TextPrimary
            ) {
                tabs.forEachIndexed { idx, tab ->
                    val selected = currentTab == idx
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = idx },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = AccentGreen,
                            indicatorColor = AccentGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    uiState = uiState,
                    onNavigateToAnalyzer = { currentTab = 1 },
                    onNavigateToArchive = { currentTab = 2 },
                    onNavigateToRepertoire = { currentTab = 3 },
                    onLoadSamplePgn = { pgn ->
                        chessViewModel.loadPgnGame(pgn, autoAnalyze = true)
                    },
                    onOpenSavedGame = { record ->
                        chessViewModel.openSavedGame(record)
                        currentTab = 1
                    }
                )
                1 -> AnalyzerScreen(
                    uiState = uiState,
                    onSquareClick = chessViewModel::onSquareClicked,
                    onPieceDraggedMove = chessViewModel::onPieceDraggedMove,
                    onSelectPromotion = chessViewModel::completePromotion,
                    onCancelPromotion = chessViewModel::cancelPromotion,
                    onStepForward = chessViewModel::stepForward,
                    onStepBackward = chessViewModel::stepBackward,
                    onJumpToStart = chessViewModel::jumpToStart,
                    onJumpToEnd = chessViewModel::jumpToEnd,
                    onJumpToPly = chessViewModel::jumpToPly,
                    onFlipBoard = chessViewModel::flipBoard,
                    onToggleAnnotationMode = chessViewModel::toggleAnnotationMode,
                    onClearAnnotations = chessViewModel::clearAnnotations,
                    onRunAnalysis = chessViewModel::runFullGameAnalysis,
                    onCancelAnalysis = chessViewModel::cancelAnalysis,
                    onSaveGame = { chessViewModel.saveCurrentGameToSqlite() },
                    onStopPractice = chessViewModel::stopRepertoirePractice
                )
                2 -> GamesArchiveScreen(
                    uiState = uiState,
                    onOpenGame = { record ->
                        chessViewModel.openSavedGame(record)
                        currentTab = 1
                    },
                    onToggleFavorite = chessViewModel::toggleFavoriteGame,
                    onDeleteGame = chessViewModel::deleteSavedGame,
                    onImportPgn = { pgn ->
                        chessViewModel.loadPgnGame(pgn, autoAnalyze = true)
                        currentTab = 1
                    },
                    onImportFen = { fen ->
                        chessViewModel.loadFenPosition(fen)
                        currentTab = 1
                    },
                    onImportOnline = { platform, user ->
                        chessViewModel.importFromOnlinePlatform(platform, user)
                    },
                    onSearchChessComPlayer = chessViewModel::searchChessComPlayer,
                    onLoadMoreChessComArchives = chessViewModel::loadMoreChessComArchives,
                    onLoadAllChessComArchives = chessViewModel::loadAllRemainingChessComArchives,
                    onSelectChessComArchiveUrl = chessViewModel::selectChessComArchiveUrl,
                    onUpdateChessComFilters = chessViewModel::updateChessComFilters,
                    onSelectChessComGame = { game ->
                        chessViewModel.importSingleChessComGame(game)
                        currentTab = 1
                    }
                )
                3 -> RepertoireScreen(
                    repertoire = uiState.repertoire,
                    onStartPractice = { entry ->
                        chessViewModel.startRepertoirePractice(entry)
                        currentTab = 1
                    },
                    onAddRepertoire = chessViewModel::saveRepertoireLine,
                    onDeleteRepertoire = chessViewModel::deleteRepertoireLine
                )
                4 -> SettingsScreen(
                    settings = uiState.settings,
                    onUpdateSettings = chessViewModel::updateSettings,
                    onApplyThemePreset = chessViewModel::applyBoardThemePreset,
                    onResetSettings = chessViewModel::resetSettings
                )
            }
        }
    }
}
