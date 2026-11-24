package com.example.ui.screens.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AppPreferences
import com.example.domain.model.DebtDirection
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PlanningHubState(
    val activeAccountsCount: Int = 0,
    val totalBalancePaise: Long = 0L,
    val activeBudgetsCount: Int = 0,
    val activeGoalsCount: Int = 0,
    val totalGoalSavedPaise: Long = 0L,
    val activeRecurringCount: Int = 0,
    val receivablePaise: Long = 0L,
    val payablePaise: Long = 0L,
    val categoriesCount: Int = 0,
    val preferences: AppPreferences = AppPreferences(),
)

class PlanningViewModel(
    financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val firstBundle = combine(
        financeRepository.accountsWithBalanceFlow,
        financeRepository.budgetsFlow,
        financeRepository.goalsWithProgressFlow,
        financeRepository.recurringPaymentsFlow,
        financeRepository.debtsWithBalanceFlow,
    ) { accounts, budgets, goals, recurring, debts ->
        val activeAccs = accounts.filter { !it.account.isArchived }
        val activeGoals = goals.filter { !it.goal.isCompleted }
        val receivable = debts
            .filter { !it.debt.isSettled && it.debt.direction == DebtDirection.LENT.name }
            .sumOf { it.outstandingAmountPaise }
        val payable = debts
            .filter { !it.debt.isSettled && it.debt.direction == DebtDirection.BORROWED.name }
            .sumOf { it.outstandingAmountPaise }
        PlanningHubState(
            activeAccountsCount = activeAccs.size,
            totalBalancePaise = activeAccs.sumOf { it.currentBalancePaise },
            activeBudgetsCount = budgets.count { it.isActive },
            activeGoalsCount = activeGoals.size,
            totalGoalSavedPaise = activeGoals.sumOf { it.savedAmountPaise },
            activeRecurringCount = recurring.count { it.isActive },
            receivablePaise = receivable,
            payablePaise = payable,
        )
    }

    val uiState: StateFlow<PlanningHubState> = combine(
        firstBundle,
        financeRepository.categoriesFlow,
        preferencesRepository.preferencesFlow,
    ) { hub, categories, prefs ->
        hub.copy(
            categoriesCount = categories.count { !it.isArchived },
            preferences = prefs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlanningHubState(),
    )

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlanningViewModel(financeRepository, preferencesRepository) as T
        }
    }
}

@Composable
fun PlanningScreen(
    viewModel: PlanningViewModel,
    onOpenAccounts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Column {
                            Text(
                            text = "Planning",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Accounts, budgets, goals, bills & debts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            }

            item {
                PlanningTile(
                    title = "Accounts & Wallets",
                    subtitle = "${state.activeAccountsCount} active • Net ${MoneyFormatter.formatWithMode(state.totalBalancePaise, state.preferences.currencyDisplayMode, false)}",
                    icon = Icons.Rounded.AccountBalance,
                    accent = financeColors.transfer,
                    testTag = "planning_accounts_card",
                    onClick = onOpenAccounts,
                )
            }
            item {
                PlanningTile(
                    title = "Budgets",
                    subtitle = "${state.activeBudgetsCount} active monthly, weekly & category limits",
                    icon = Icons.Rounded.PieChart,
                    accent = financeColors.warning,
                    testTag = "planning_budgets_card",
                    onClick = onOpenBudgets,
                )
            }
            item {
                PlanningTile(
                    title = "Savings Goals",
                    subtitle = "${state.activeGoalsCount} active • ${MoneyFormatter.formatWithMode(state.totalGoalSavedPaise, state.preferences.currencyDisplayMode, false)} saved",
                    icon = Icons.Rounded.Savings,
                    accent = financeColors.income,
                    testTag = "planning_goals_card",
                    onClick = onOpenGoals,
                )
            }
            item {
                PlanningTile(
                    title = "Recurring Payments & Bills",
                    subtitle = "${state.activeRecurringCount} scheduled commitments & reminders",
                    icon = Icons.Rounded.Repeat,
                    accent = financeColors.expense,
                    testTag = "planning_recurring_card",
                    onClick = onOpenRecurring,
                )
            }
            item {
                PlanningTile(
                    title = "Debts & Liabilities",
                    subtitle = "Receivable ${MoneyFormatter.formatWithMode(state.receivablePaise, state.preferences.currencyDisplayMode, false)} • Owed ${MoneyFormatter.formatWithMode(state.payablePaise, state.preferences.currencyDisplayMode, false)}",
                    icon = Icons.Rounded.Handshake,
                    accent = financeColors.debt,
                    testTag = "planning_debts_card",
                    onClick = onOpenDebts,
                )
            }
            item {
                PlanningTile(
                    title = "Categories & Subcategories",
                    subtitle = "${state.categoriesCount} active income & expense categories",
                    icon = Icons.Rounded.Category,
                    accent = MaterialTheme.colorScheme.primary,
                    testTag = "planning_categories_card",
                    onClick = onOpenCategories,
                )
            }
        }
    }
}

@Composable
private fun PlanningTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
