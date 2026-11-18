package com.nikanokh.chessinsightanalyzermobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgPrimary = Color(0xFF161512)
val BgSecondary = Color(0xFF262421)
val BgCard = Color(0xFF302E2B)
val BgElevated = Color(0xFF383632)
val AccentGreen = Color(0xFF779954)
val AccentTeal = Color(0xFF1BACA6)
val AccentGold = Color(0xFFF0C15C)
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

// Aliases for compatibility across screens
val ChessBackground = BgPrimary
val ChessSurface = BgSecondary
val ChessSurfaceElevated = BgCard
val ChessBorder = Color(0xFF3B3834)
val ChessGreenPrimary = AccentGreen
val ChessGoldEco = Color(0xFFA88865)
val ChessTextPrimary = TextPrimary
val ChessTextSecondary = TextSecondary
val ChessDanger = Color(0xFFCA3431)

private val DarkChessColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentTeal,
    tertiary = AccentGold,
    background = BgPrimary,
    surface = BgSecondary,
    surfaceVariant = BgCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun ChessInsightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkChessColorScheme,
        content = content
    )
}
