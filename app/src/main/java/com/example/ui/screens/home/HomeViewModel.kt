package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.AccountWithBalance
import com.example.data.repository.FinanceRepository
import com.example.data.repository.GoalWithProgress
import com.example.data.repository.toInput
import com.example.domain.finance.BudgetUtilizationResult
import com.example.domain.finance.CategoryBreakdownItem
import com.example.domain.finance.DashboardCalculator
import com.example.domain.finance.DashboardSummary
import com.example.domain.finance.DateRangeUtils
import com.example.domain.finance.PeriodIncomeExpensePoint
import com.example.domain.model.AppPreferences
import com.example.domain.model.BudgetPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HomeFilterState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val filter: HomeFilterState = HomeFilterState(),
    val preferences: AppPreferences = AppPreferences(),
    val summary: DashboardSummary = DashboardSummary(0, 0, 0, 0, 0, 0, 0, 0, 0),
    val accounts: List<AccountWithBalance> = emptyList(),
    val accountMap: Map<Long, AccountEntity> = emptyMap(),
    val categoryMap: Map<Long, CategoryEntity> = emptyMap(),
    val monthlySeries: List<PeriodIncomeExpensePoint> = emptyList(),
    val expenseBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingBills: List<RecurringPaymentEntity> = emptyList(),
    val budgets: List<BudgetUtilizationResult> = emptyList(),
    val goals: List<GoalWithProgress> = emptyList(),
    val bannerMessage: String? = null,
)

class HomeViewModel(
    private val financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(HomeFilterState())
    private val messageFlow = MutableStateFlow<String?>(null)

    private val coreDataFlow = combine(
        financeRepository.accountsWithBalanceFlow,
        financeRepository.categoriesFlow,
        financeRepository.transactionsFlow,
        financeRepository.budgetsFlow,
        financeRepository.goalsWithProgressFlow,
    ) { accounts, categories, transactions, budgets, goals ->
        CoreBundle(accounts, categories, transactions, budgets, goals)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreDataFlow,
        financeRepository.recurringPaymentsFlow,
        preferencesRepository.preferencesFlow,
        filterFlow,
        messageFlow,
    ) { core, recurring, prefs, filter, message ->
        val accountsInput = core.accounts.map { it.account.toInput() }
        val balancesMap = core.accounts.associate { it.account.id to it.currentBalancePaise }
        val categoryMap = core.categories.associateBy { it.id }
        val accountMap = core.accounts.associate { it.account.id to it.account }

        val (startMs, endMs) = if (filter.customStartDate != null && filter.customEndDate != null) {
            DateRangeUtils.toEpochMillisRange(filter.customStartDate, filter.customEndDate)
        } else {
            DateRangeUtils.monthRange(filter.selectedMonth)
        }

        val allTxInputs = core.transactions.map { it.toInput() }
        val periodTxEntities = core.transactions.filter { it.timestamp in startMs..endMs }
        val periodTxInputs = periodTxEntities.map { it.toInput() }

        val summary = DashboardCalculator.calculateSummary(
            accounts = accountsInput,
            accountBalances = balancesMap,
            periodTransactions = periodTxInputs,
        )

        val last6Months = (5L downTo 0L).map { filter.selectedMonth.minusMonths(it) }
        val monthlySeries = DashboardCalculator.calculateMonthlySeries(allTxInputs, last6Months)

        val categoryLookup = core.categories.associate {
            it.id to Triple(it.name, it.colorHex, it.iconName)
        }
        val breakdown = DashboardCalculator.calculateExpenseByCategory(periodTxInputs, categoryLookup)

        val today = LocalDate.now()
        val budgetUtils = core.budgets.filter { it.isActive }.map { b ->
            val period = runCatching { BudgetPeriod.valueOf(b.periodType) }.getOrDefault(BudgetPeriod.MONTHLY)
            val (bStart, bEnd) = if (period == BudgetPeriod.MONTHLY && filter.customStartDate == null) {
                filter.selectedMonth.atDay(1) to filter.selectedMonth.atEndOfMonth()
            } else {
                DateRangeUtils.budgetPeriodDates(period, today, b.customStartEpochDay, b.customEndEpochDay)
            }
            val (bStartMs, bEndMs) = DateRangeUtils.toEpochMillisRange(bStart, bEnd)
            val bTx = allTxInputs.filter { it.timestamp in bStartMs..bEndMs }
            DashboardCalculator.calculateBudgetUtilization(
                budgetId = b.id,
                name = b.name,
                categoryId = b.categoryId,
                limitPaise = b.limitAmountPaise,
                alertThresholdPercent = b.alertThresholdPercent,
                periodTransactions = bTx,
            )
        }.sortedByDescending { it.utilizationPercent }

        val upcoming = recurring.filter { it.isActive }
            .sortedBy { it.nextDueDateEpochDay }
            .take(4)

        HomeUiState(
            isLoading = false,
            filter = filter,
            preferences = prefs,
            summary = summary,
            accounts = core.accounts.filter { !it.account.isArchived },
            accountMap = accountMap,
            categoryMap = categoryMap,
            monthlySeries = monthlySeries,
            expenseBreakdown = breakdown,
            recentTransactions = periodTxEntities.take(6),
            upcomingBills = upcoming,
            budgets = budgetUtils.take(4),
            goals = core.goals.filter { !it.goal.isCompleted }.take(4),
            bannerMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun previousMonth() {
        filterFlow.update {
            it.copy(
                selectedMonth = it.selectedMonth.minusMonths(1),
                customStartDate = null,
                customEndDate = null,
            )
        }
    }

    fun nextMonth() {
        filterFlow.update {
            it.copy(
                selectedMonth = it.selectedMonth.plusMonths(1),
                customStartDate = null,
                customEndDate = null,
            )
        }
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        val orderedStart = if (start.isAfter(end)) end else start
        val orderedEnd = if (start.isAfter(end)) start else end
        filterFlow.update {
            it.copy(customStartDate = orderedStart, customEndDate = orderedEnd)
        }
    }

    fun clearCustomDateRange() {
        filterFlow.update { it.copy(customStartDate = null, customEndDate = null) }
    }

    fun confirmUpcomingBill(item: RecurringPaymentEntity) {
        viewModelScope.launch {
            val created = financeRepository.confirmRecurringOccurrence(item)
            messageFlow.value = if (created) {
                "Logged '${item.title}' and scheduled next occurrence."
            } else {
                "This occurrence was already logged."
            }
        }
    }

    fun skipUpcomingBill(item: RecurringPaymentEntity) {
        viewModelScope.launch {
            financeRepository.skipRecurringOccurrence(item)
            messageFlow.value = "Skipped current occurrence of '${item.title}'."
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    private data class CoreBundle(
        val accounts: List<AccountWithBalance>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<com.example.data.local.BudgetEntity>,
        val goals: List<GoalWithProgress>,
    )

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(financeRepository, preferencesRepository) as T
        }
    }
}
