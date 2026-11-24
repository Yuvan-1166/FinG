package com.example.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenInsights: () -> Unit,
    onOpenPlanning: () -> Unit,
    onOpenStocks: () -> Unit,
    onOpenGold: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "More & Wealth",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Analytics, planning, stocks & preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = "Primary Modules",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        MoreNavRow(
                            title = "Insights & Analytics",
                            subtitle = "Cashflow trends, category ratios & net worth",
                            icon = Icons.Rounded.Analytics,
                            accent = financeColors.transfer,
                            testTag = "more_insights",
                            onClick = onOpenInsights,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow(
                            title = "Planning Hub",
                            subtitle = "Accounts, budgets, goals, bills & debts",
                            icon = Icons.Rounded.DashboardCustomize,
                            accent = financeColors.income,
                            testTag = "more_planning",
                            onClick = onOpenPlanning,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow(
                            title = "Stock Investments",
                            subtitle = "Manual equity holdings, average cost & realized P&L",
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            accent = MaterialTheme.colorScheme.primary,
                            testTag = "more_stocks",
                            onClick = onOpenStocks,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow(
                            title = "Gold Treasury",
                            subtitle = "Physical holdings, rates, obligations & valuation",
                            icon = Icons.Rounded.Diamond,
                            accent = ChampagneGold,
                            testTag = "more_gold",
                            onClick = onOpenGold,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow(
                            title = "Settings & Data Backup",
                            subtitle = "Theme, currency formatting, app lock, CSV/JSON",
                            icon = Icons.Rounded.Settings,
                            accent = MaterialTheme.colorScheme.onSurfaceVariant,
                            testTag = "more_settings",
                            onClick = onOpenSettings,
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Quick Finance Shortcuts",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        MoreNavRow("Accounts", "Banks, cash, UPI & cards", Icons.Rounded.AccountBalance, financeColors.transfer, onClick = onOpenAccounts)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow("Budgets", "Monthly & category limits", Icons.Rounded.PieChart, financeColors.warning, onClick = onOpenBudgets)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow("Savings Goals", "Dedicated targets & milestones", Icons.Rounded.Savings, financeColors.income, onClick = onOpenGoals)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow("Recurring Bills", "EMIs, rent & subscriptions", Icons.Rounded.EventRepeat, financeColors.transfer, onClick = onOpenRecurring)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow("Debts", "Money lent & borrowed ledger", Icons.Rounded.Handshake, financeColors.debt, onClick = onOpenDebts)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MoreNavRow("Categories", "Income & expense taxonomy", Icons.Rounded.Category, financeColors.expense, onClick = onOpenCategories)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
