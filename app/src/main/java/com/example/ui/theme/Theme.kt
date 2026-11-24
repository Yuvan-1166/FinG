package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Navy950,
    primaryContainer = EmeraldTintDark,
    onPrimaryContainer = Emerald200,
    secondary = TransferBlueDark,
    onSecondary = Navy950,
    secondaryContainer = TransferTintDark,
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = WarningAmberDark,
    onTertiary = Navy950,
    tertiaryContainer = WarningTintDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = Navy900,
    onBackground = Color(0xFFF1F5F9),
    surface = Navy850,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Navy800,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Navy950,
    surfaceContainerLow = Navy850,
    surfaceContainer = Navy800,
    surfaceContainerHigh = Navy700,
    surfaceContainerHighest = Navy600,
    outline = Color(0xFF28374E),
    outlineVariant = Color(0xFF1D2A3E),
    error = ExpenseRedDark,
    onError = Navy950,
    errorContainer = ExpenseTintDark,
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = EmeraldTintLight,
    onPrimaryContainer = Emerald900,
    secondary = TransferBlue,
    onSecondary = Color.White,
    secondaryContainer = TransferTintLight,
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = WarningAmber,
    onTertiary = Color.White,
    tertiaryContainer = WarningTintLight,
    onTertiaryContainer = Color(0xFF78350F),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    surfaceContainerHighest = Slate300,
    outline = Slate300,
    outlineVariant = Slate200,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseTintLight,
    onErrorContainer = Color(0xFF7F1D1D),
)

@Immutable
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val debt: Color,
    val debtContainer: Color,
    val cardElevated: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

private val DarkFinanceColors = FinanceColors(
    income = Emerald400,
    incomeContainer = EmeraldTintDark,
    expense = ExpenseRedDark,
    expenseContainer = ExpenseTintDark,
    transfer = TransferBlueDark,
    transferContainer = TransferTintDark,
    warning = WarningAmberDark,
    warningContainer = WarningTintDark,
    debt = DebtVioletDark,
    debtContainer = DebtTintDark,
    cardElevated = Navy800,
    heroStart = Color(0xFF0E1B2E),
    heroEnd = Color(0xFF0B2523),
)

private val LightFinanceColors = FinanceColors(
    income = Emerald600,
    incomeContainer = EmeraldTintLight,
    expense = ExpenseRed,
    expenseContainer = ExpenseTintLight,
    transfer = TransferBlue,
    transferContainer = TransferTintLight,
    warning = WarningAmber,
    warningContainer = WarningTintLight,
    debt = DebtViolet,
    debtContainer = DebtTintLight,
    cardElevated = Color.White,
    heroStart = Color(0xFF0B192C),
    heroEnd = Color(0xFF082E26),
)

val LocalFinanceColors = staticCompositionLocalOf { DarkFinanceColors }

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RupeeLedgerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val financeColors = if (isDark) DarkFinanceColors else LightFinanceColors

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
