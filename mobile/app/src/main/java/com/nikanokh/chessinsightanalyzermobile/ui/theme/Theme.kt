package com.nikanokh.chessinsightanalyzermobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ChessBackground = Color(0xFF1E1C1A)
val ChessSurface = Color(0xFF262421)
val ChessSurfaceElevated = Color(0xFF312E2B)
val ChessBorder = Color(0xFF3B3834)
val ChessGreenPrimary = Color(0xFF779954)
val ChessGoldEco = Color(0xFFA88865)
val ChessTextPrimary = Color(0xFFEBEBEB)
val ChessTextSecondary = Color(0xFF9E9B95)
val ChessDanger = Color(0xFFE05252)

private val DarkChessColorScheme = darkColorScheme(
    primary = ChessGreenPrimary,
    onPrimary = Color.White,
    secondary = ChessGoldEco,
    onSecondary = Color.White,
    background = ChessBackground,
    onBackground = ChessTextPrimary,
    surface = ChessSurface,
    onSurface = ChessTextPrimary,
    surfaceVariant = ChessSurfaceElevated,
    onSurfaceVariant = ChessTextSecondary,
    error = ChessDanger,
    onError = Color.White
)

@Composable
fun ChessInsightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkChessColorScheme,
        content = content
    )
}
