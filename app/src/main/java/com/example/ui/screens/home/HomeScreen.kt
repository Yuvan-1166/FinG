package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.BudgetStatus
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.MoneyText
import com.example.ui.components.SectionHeader
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddTransaction: () -> Unit,
    onOpenTransactionDetail: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDateRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.bannerMessage) {
        state.bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "RupeeLedger",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Personal Finance Overview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showDateRangePicker = true },
                            modifier = Modifier.testTag("home_date_filter_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Custom date filter",
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("home_settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                            )
                        }
                    }
                }
            }

            // Month Selector / Active Custom Filter
            item {
                MonthSelectorBar(
                    state = state,
                    onPrevMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    onClearCustom = viewModel::clearCustomDateRange,
                )
            }

            // Hero Balance Card
            item {
                HeroBalanceCard(
                    state = state,
                    heroStart = financeColors.heroStart,
                    heroEnd = financeColors.heroEnd,
                    onOpenAccounts = onOpenAccounts,
                )
            }

            // Monthly Income / Expense / Net Savings / Savings Rate
            item {
                MonthlyCashflowGrid(state = state)
            }

            // Onboarding prompt if user has no accounts yet
            if (state.accounts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Create your first account",
                        subtitle = "Add a bank account, UPI wallet, cash, or credit card to start tracking your INR balances.",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        actionLabel = "Add Account",
                        actionTestTag = "home_create_first_account_button",
                        onActionClick = onOpenAccounts,
                    )
                }
            }

            // Income vs Expense Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "Income vs Expenses")
                        Spacer(Modifier.height(10.dp))
                        IncomeExpenseBarChart(
                            points = state.monthlySeries,
                            preferences = state.preferences,
                        )
                    }
                }
            }

            // Expense Category Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "Expense Breakdown")
                        Spacer(Modifier.height(10.dp))
                        CategoryDonutChart(
                            items = state.expenseBreakdown,
                            preferences = state.preferences,
                        )
                    }
                }
            }

            // Upcoming Bills
            item {
                SectionHeader(
                    title = "Upcoming Bills",
                    actionLabel = "Manage",
                    onActionClick = onOpenRecurring,
                )
            }
            item {
                if (state.upcomingBills.isEmpty()) {
                    EmptyStateCard(
                        title = "No upcoming bills",
                        subtitle = "Track rent, electricity, internet, EMI, and subscriptions with due reminders.",
                        icon = Icons.Rounded.NotificationsActive,
                        actionLabel = "Add Recurring Bill",
                        onActionClick = onOpenRecurring,
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val todayEpoch = LocalDate.now().toEpochDay()
                            state.upcomingBills.forEachIndexed { idx, bill ->
                                val daysUntil = bill.nextDueDateEpochDay - todayEpoch
                                val dueLabel = when {
                                    daysUntil < 0 -> "Overdue by ${-daysUntil}d"
                                    daysUntil == 0L -> "Due today"
                                    else -> "Due ${formatEpochDay(bill.nextDueDateEpochDay, state.preferences.dateFormat)}"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bill.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = dueLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (daysUntil <= 0) financeColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    MoneyText(
                                        paise = bill.amountPaise,
                                        preferences = state.preferences,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.confirmUpcomingBill(bill) },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Confirm payment",
                                            tint = financeColors.income,
                                        )
                                    }
                                }
                                if (idx < state.upcomingBills.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Budget Utilization
            item {
                SectionHeader(
                    title = "Budget Utilization",
                    actionLabel = "Budgets",
                    onActionClick = onOpenBudgets,
                )
            }
            item {
                if (state.budgets.isEmpty()) {
                    EmptyStateCard(
                        title = "No budgets configured",
                        subtitle = "Set monthly or category spending caps to monitor utilization in real time.",
                        actionLabel = "Create Budget",
                        onActionClick = onOpenBudgets,
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            state.budgets.forEach { b ->
                                val barColor = when (b.status) {
                                    BudgetStatus.EXCEEDED -> financeColors.expense
                                    BudgetStatus.APPROACHING -> financeColors.warning
                                    BudgetStatus.NORMAL -> financeColors.income
                                }
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = b.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${b.utilizationPercent}%",
                                            style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                            color = barColor,
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (b.utilizationPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "Spent ${MoneyFormatter.formatWithMode(b.spentPaise, state.preferences.currencyDisplayMode, false)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = if (b.remainingPaise >= 0) {
                                                "Left ${MoneyFormatter.formatWithMode(b.remainingPaise, state.preferences.currencyDisplayMode, false)}"
                                            } else {
                                                "Over by ${MoneyFormatter.formatWithMode(-b.remainingPaise, state.preferences.currencyDisplayMode, false)}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (b.remainingPaise < 0) financeColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Savings Goals
            item {
                SectionHeader(
                    title = "Savings Goals",
                    actionLabel = "Goals",
                    onActionClick = onOpenGoals,
                )
            }
            item {
                if (state.goals.isEmpty()) {
                    EmptyStateCard(
                        title = "Set a savings goal",
                        subtitle = "Save intentionally for an emergency fund, vacation, gadget, or education.",
                        icon = Icons.Rounded.Savings,
                        actionLabel = "Add Goal",
                        onActionClick = onOpenGoals,
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.goals.forEach { g ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(g.goal.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${g.progressPercent}%",
                                            style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                            color = financeColors.income,
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (g.progressPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = financeColors.income,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${MoneyFormatter.formatWithMode(g.savedAmountPaise, state.preferences.currencyDisplayMode, false)} of ${MoneyFormatter.formatWithMode(g.goal.targetAmountPaise, state.preferences.currencyDisplayMode, false)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    actionLabel = "See All",
                    onActionClick = onOpenActivity,
                )
            }
            item {
                if (state.recentTransactions.isEmpty()) {
                    EmptyStateCard(
                        title = "No transactions for this period",
                        subtitle = "Log an income, expense, or transfer using the + button below.",
                        icon = Icons.Rounded.Add,
                        actionLabel = "Add Transaction",
                        actionTestTag = "home_empty_add_tx_button",
                        onActionClick = onAddTransaction,
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column {
                            state.recentTransactions.forEachIndexed { idx, tx ->
                                TransactionRow(
                                    transaction = tx,
                                    account = state.accountMap[tx.accountId],
                                    destinationAccount = tx.destinationAccountId?.let { state.accountMap[it] },
                                    category = tx.categoryId?.let { state.categoryMap[it] },
                                    preferences = state.preferences,
                                    onClick = { onOpenTransactionDetail(tx.id) },
                                )
                                if (idx < state.recentTransactions.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis ?: s
                        if (s != null && e != null) {
                            val zone = ZoneId.systemDefault()
                            val startDate = Instant.ofEpochMilli(s).atZone(zone).toLocalDate()
                            val endDate = Instant.ofEpochMilli(e).atZone(zone).toLocalDate()
                            viewModel.setCustomDateRange(startDate, endDate)
                        }
                        showDateRangePicker = false
                    },
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(420.dp))
        }
    }
}

@Composable
private fun MonthSelectorBar(
    state: HomeUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onClearCustom: () -> Unit,
) {
    if (state.filter.customStartDate != null && state.filter.customEndDate != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = onClearCustom,
                label = {
                    val f = DateTimeFormatter.ofPattern("dd MMM")
                    Text("${state.filter.customStartDate.format(f)} - ${state.filter.customEndDate.format(f)}")
                },
                trailingIcon = {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear filter", modifier = Modifier.size(16.dp))
                },
            )
            TextButton(onClick = onClearCustom) { Text("Back to Monthly") }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.testTag("home_prev_month_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = state.filter.selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("home_next_month_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    state: HomeUiState,
    heroStart: Color,
    heroEnd: Color,
    onOpenAccounts: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(heroStart, heroEnd)))
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total Available Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Surface(
                    onClick = onOpenAccounts,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = "${state.accounts.size} Accounts",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            MoneyText(
                paise = state.summary.totalAvailableBalancePaise,
                preferences = state.preferences,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                forceFull = true,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Net Worth",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    MoneyText(
                        paise = state.summary.netWorthPaise,
                        preferences = state.preferences,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFA7F3D0),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Credit Card Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    MoneyText(
                        paise = state.summary.creditCardLiabilityPaise,
                        preferences = state.preferences,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (state.summary.creditCardLiabilityPaise > 0L) Color(0xFFFCA5A5) else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyCashflowGrid(state: HomeUiState) {
    val financeColors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                title = "Income",
                amountPaise = state.summary.totalIncomePaise,
                accent = financeColors.income,
                icon = Icons.Rounded.TrendingUp,
                state = state,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                title = "Expenses",
                amountPaise = state.summary.netExpensePaise,
                accent = financeColors.expense,
                icon = Icons.Rounded.TrendingDown,
                state = state,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                title = "Net Savings",
                amountPaise = state.summary.netSavingsPaise,
                accent = if (state.summary.netSavingsPaise >= 0) financeColors.income else financeColors.expense,
                icon = Icons.Rounded.Savings,
                state = state,
                modifier = Modifier.weight(1f),
                showPlus = true,
            )
            Card(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Savings Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    val tenths = state.summary.savingsRateTenthsPercent
                    val rateStr = "${tenths / 10}.${kotlin.math.abs(tenths % 10)}%"
                    Text(
                        text = rateStr,
                        style = MaterialTheme.typography.titleLarge.merge(MoneyTextStyle),
                        color = if (tenths >= 0) financeColors.income else financeColors.expense,
                    )
                    if (state.summary.totalRefundPaise > 0L) {
                        Text(
                            text = "Refunds: ${MoneyFormatter.formatWithMode(state.summary.totalRefundPaise, state.preferences.currencyDisplayMode, false)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    amountPaise: Long,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    state: HomeUiState,
    modifier: Modifier = Modifier,
    showPlus: Boolean = false,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            MoneyText(
                paise = amountPaise,
                preferences = state.preferences,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                showPlusSign = showPlus,
            )
        }
    }
}
