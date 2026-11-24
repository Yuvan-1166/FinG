package com.example.ui.screens.insights

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.BudgetStatus
import com.example.domain.model.DateRangePreset
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.MoneyText
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingTrendChart
import com.example.ui.components.TransactionRow
import com.example.ui.components.parseHexColor
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onOpenTransactionDetail: (Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    var showCustomPicker by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = "Insights & Analytics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Date Filter Presets
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateRangePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.filter.preset == preset,
                            onClick = {
                                if (preset == DateRangePreset.CUSTOM) {
                                    showCustomPicker = true
                                } else {
                                    viewModel.setPreset(preset)
                                }
                            },
                            label = { Text(preset.label) },
                        )
                    }
                }
            }

            // Summary & Month-over-Month Comparison
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = "Period Cashflow")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(state.summary.totalIncomePaise, state.preferences, style = MaterialTheme.typography.titleMedium, color = financeColors.income)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(state.summary.netExpensePaise, state.preferences, style = MaterialTheme.typography.titleMedium, color = financeColors.expense)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Savings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(
                                    state.summary.netSavingsPaise,
                                    state.preferences,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (state.summary.netSavingsPaise >= 0) financeColors.income else financeColors.expense,
                                    showPlusSign = true,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Month-over-Month Spending", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Prev: ${MoneyFormatter.formatWithMode(state.prevMonthExpensePaise, state.preferences.currencyDisplayMode, false)} • Now: ${MoneyFormatter.formatWithMode(state.currentMonthExpensePaise, state.preferences.currencyDisplayMode, false)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            val momText = state.momChangePercent?.let { pct ->
                                if (pct >= 0) "+$pct%" else "$pct%"
                            } ?: "—"
                            Text(
                                text = momText,
                                style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                color = when {
                                    state.momChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    state.momChangePercent!! > 0 -> financeColors.expense
                                    else -> financeColors.income
                                },
                            )
                        }
                    }
                }
            }

            // Monthly Income vs Expense
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "6-Month Income vs Expenses")
                        Spacer(Modifier.height(10.dp))
                        IncomeExpenseBarChart(points = state.monthlySeries, preferences = state.preferences)
                    }
                }
            }

            // Daily / Weekly Spending Trend
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Spending Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = !state.filter.showWeeklyTrend,
                                    onClick = { viewModel.setShowWeeklyTrend(false) },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                ) { Text("Daily", style = MaterialTheme.typography.labelSmall) }
                                SegmentedButton(
                                    selected = state.filter.showWeeklyTrend,
                                    onClick = { viewModel.setShowWeeklyTrend(true) },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                ) { Text("Weekly", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        SpendingTrendChart(
                            points = state.spendingTrend,
                            preferences = state.preferences,
                            lineColor = financeColors.expense,
                        )
                    }
                }
            }

            // Category Breakdown & Highest Categories
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = "Highest Spending Categories")
                        CategoryDonutChart(items = state.categoryBreakdown, preferences = state.preferences)
                        state.categoryBreakdown.forEach { cat ->
                            val color = parseHexColor(cat.colorHex)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium)
                                    MoneyText(cat.amountPaise, state.preferences, style = MaterialTheme.typography.labelLarge)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (cat.shareBasisPoints / 10000f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Recurring Burn Rate & Account Overview
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Monthly Recurring Burn", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            MoneyText(state.estimatedMonthlyRecurringExpensePaise, state.preferences, style = MaterialTheme.typography.titleMedium, color = financeColors.warning)
                            Text("${state.activeRecurringCount} active commitments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Savings Rate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            val tenths = state.summary.savingsRateTenthsPercent
                            Text(
                                text = "${tenths / 10}.${kotlin.math.abs(tenths % 10)}%",
                                style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                color = if (tenths >= 0) financeColors.income else financeColors.expense,
                            )
                            Text("Across ${state.accounts.size} active accounts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Largest Individual Transactions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = "Largest Individual Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.largestTransactions.isEmpty()) {
                            EmptyStateCard(
                                title = "No expenses in range",
                                subtitle = "Add expenses to see your top transactions here.",
                                icon = Icons.Rounded.Analytics,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        } else {
                            state.largestTransactions.forEachIndexed { idx, tx ->
                                TransactionRow(
                                    transaction = tx,
                                    account = state.accountMap[tx.accountId],
                                    destinationAccount = null,
                                    category = tx.categoryId?.let { state.categoryMap[it] },
                                    preferences = state.preferences,
                                    onClick = { onOpenTransactionDetail(tx.id) },
                                )
                                if (idx < state.largestTransactions.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Account Balances Overview & Budget Utilization
            if (state.accounts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(title = "Account Balances")
                            state.accounts.forEach { acc ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(acc.account.name, style = MaterialTheme.typography.bodyMedium)
                                    MoneyText(
                                        acc.currentBalancePaise,
                                        state.preferences,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (acc.currentBalancePaise < 0) financeColors.expense else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.budgets.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(title = "Active Budget Utilization")
                            state.budgets.forEach { b ->
                                val barColor = when (b.status) {
                                    BudgetStatus.EXCEEDED -> financeColors.expense
                                    BudgetStatus.APPROACHING -> financeColors.warning
                                    BudgetStatus.NORMAL -> financeColors.income
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(b.name, style = MaterialTheme.typography.bodyMedium)
                                    Text("${b.utilizationPercent}%", style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle), color = barColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomPicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis ?: s
                        if (s != null && e != null) {
                            val zone = ZoneId.systemDefault()
                            viewModel.setCustomRange(
                                Instant.ofEpochMilli(s).atZone(zone).toLocalDate(),
                                Instant.ofEpochMilli(e).atZone(zone).toLocalDate(),
                            )
                        }
                        showCustomPicker = false
                    },
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) { Text("Cancel") }
            },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(420.dp))
        }
    }
}
