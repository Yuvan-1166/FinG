package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Deep Charcoal / Obsidian Foundations
val Charcoal950 = Color(0xFF070709)
val Charcoal900 = Color(0xFF0D0E12)
val Charcoal850 = Color(0xFF14151B)
val Charcoal800 = Color(0xFF1B1D24)
val Charcoal700 = Color(0xFF252832)

// Champagne & Bullion Gold Palette
val ChampagneGold = Color(0xFFE6C37A)
val ChampagneGoldLight = Color(0xFFF6DFAB)
val MutedGold = Color(0xFFB89758)
val DeepBronzeGold = Color(0xFF7D622E)
val GoldTintSurface = Color(0xFF231D12)
val IvoryText = Color(0xFFF6F2E9)
val WarmSlateMuted = Color(0xFFA39E93)

val GoldGainEmerald = Color(0xFF34D399)
val GoldLossCoral = Color(0xFFF87171)

private val GoldDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = Charcoal950,
    primaryContainer = GoldTintSurface,
    onPrimaryContainer = ChampagneGoldLight,
    secondary = MutedGold,
    onSecondary = Charcoal950,
    secondaryContainer = Color(0xFF24211A),
    onSecondaryContainer = ChampagneGoldLight,
    tertiary = GoldGainEmerald,
    onTertiary = Charcoal950,
    background = Charcoal900,
    onBackground = IvoryText,
    surface = Charcoal850,
    onSurface = IvoryText,
    surfaceVariant = Charcoal800,
    onSurfaceVariant = WarmSlateMuted,
    surfaceContainerLowest = Charcoal950,
    surfaceContainerLow = Charcoal850,
    surfaceContainer = Charcoal800,
    surfaceContainerHigh = Charcoal700,
    outline = Color(0xFF353128),
    outlineVariant = Color(0xFF26231D),
    error = GoldLossCoral,
    onError = Charcoal950,
    errorContainer = Color(0xFF34181D),
    onErrorContainer = Color(0xFFFECACA),
)

@Immutable
data class GoldColors(
    val champagne: Color,
    val champagneBright: Color,
    val mutedGold: Color,
    val goldBorder: Color,
    val cardElevated: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val gain: Color,
    val loss: Color,
    val chartPalette: List<Color>,
)

private val DefaultGoldColors = GoldColors(
    champagne = ChampagneGold,
    champagneBright = ChampagneGoldLight,
    mutedGold = MutedGold,
    goldBorder = Color(0xFF423620),
    cardElevated = Charcoal800,
    heroStart = Color(0xFF17140E),
    heroEnd = Color(0xFF0E0E13),
    gain = GoldGainEmerald,
    loss = GoldLossCoral,
    chartPalette = listOf(
        Color(0xFFE6C37A), // 24K Champagne
        Color(0xFFC89B3C), // 22K Sovereign
        Color(0xFF9C7834), // 18K Antique
        Color(0xFF6E572E), // 14K Bronze
        Color(0xFFF3DEAE), // Highlight Gold
        Color(0xFF52B788), // Accent Emerald
    ),
)

val LocalGoldColors = staticCompositionLocalOf { DefaultGoldColors }

@Composable
fun GoldTreasuryTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGoldColors provides DefaultGoldColors) {
        MaterialTheme(
            colorScheme = GoldDarkColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
