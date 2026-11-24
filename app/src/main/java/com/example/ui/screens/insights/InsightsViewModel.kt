package com.example.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.AccountWithBalance
import com.example.data.repository.FinanceRepository
import com.example.data.repository.toInput
import com.example.domain.finance.BudgetUtilizationResult
import com.example.domain.finance.CategoryBreakdownItem
import com.example.domain.finance.DailySpendPoint
import com.example.domain.finance.DashboardCalculator
import com.example.domain.finance.DashboardSummary
import com.example.domain.finance.DateRangeUtils
import com.example.domain.finance.PeriodIncomeExpensePoint
import com.example.domain.model.AppPreferences
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.DateRangePreset
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class InsightsFilter(
    val preset: DateRangePreset = DateRangePreset.THIS_MONTH,
    val customStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val customEnd: LocalDate = LocalDate.now(),
    val showWeeklyTrend: Boolean = false,
)

data class InsightsUiState(
    val isLoading: Boolean = true,
    val filter: InsightsFilter = InsightsFilter(),
    val preferences: AppPreferences = AppPreferences(),
    val summary: DashboardSummary = DashboardSummary(0, 0, 0, 0, 0, 0, 0, 0, 0),
    val prevMonthExpensePaise: Long = 0L,
    val currentMonthExpensePaise: Long = 0L,
    val momChangePercent: Int? = null,
    val monthlySeries: List<PeriodIncomeExpensePoint> = emptyList(),
    val spendingTrend: List<DailySpendPoint> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val largestTransactions: List<TransactionEntity> = emptyList(),
    val estimatedMonthlyRecurringExpensePaise: Long = 0L,
    val activeRecurringCount: Int = 0,
    val accounts: List<AccountWithBalance> = emptyList(),
    val budgets: List<BudgetUtilizationResult> = emptyList(),
    val accountMap: Map<Long, AccountEntity> = emptyMap(),
    val categoryMap: Map<Long, CategoryEntity> = emptyMap(),
)

class InsightsViewModel(
    financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(InsightsFilter())

    private val dataFlow = combine(
        financeRepository.accountsWithBalanceFlow,
        financeRepository.categoriesFlow,
        financeRepository.transactionsFlow,
        financeRepository.budgetsFlow,
        financeRepository.recurringPaymentsFlow,
    ) { accounts, categories, transactions, budgets, recurring ->
        Bundle(accounts, categories, transactions, budgets, recurring)
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        dataFlow,
        preferencesRepository.preferencesFlow,
        filterFlow,
    ) { bundle, prefs, filter ->
        val today = LocalDate.now()
        val (startDate, endDate) = DateRangeUtils.presetDates(
            preset = filter.preset,
            today = today,
            customStart = filter.customStart,
            customEnd = filter.customEnd,
        )
        val (startMs, endMs) = DateRangeUtils.toEpochMillisRange(startDate, endDate)

        val accountsInput = bundle.accounts.map { it.account.toInput() }
        val balancesMap = bundle.accounts.associate { it.account.id to it.currentBalancePaise }
        val allTxInputs = bundle.transactions.map { it.toInput() }
        val periodTxEntities = bundle.transactions.filter { it.timestamp in startMs..endMs }
        val periodTxInputs = periodTxEntities.map { it.toInput() }

        val summary = DashboardCalculator.calculateSummary(
            accounts = accountsInput,
            accountBalances = balancesMap,
            periodTransactions = periodTxInputs,
        )

        val currentYm = YearMonth.from(today)
        val prevYm = currentYm.minusMonths(1)
        val twoMonthSeries = DashboardCalculator.calculateMonthlySeries(allTxInputs, listOf(prevYm, currentYm))
        val prevExp = twoMonthSeries[0].expensePaise
        val currExp = twoMonthSeries[1].expensePaise
        val momPct = if (prevExp > 0L) (((currExp - prevExp) * 100L) / prevExp).toInt() else null

        val last6Months = (5L downTo 0L).map { currentYm.minusMonths(it) }
        val monthlySeries = DashboardCalculator.calculateMonthlySeries(allTxInputs, last6Months)

        val dailyRaw = DashboardCalculator.calculateDailySpendingSeries(periodTxInputs, startDate, endDate)
        val trendPoints = if (filter.showWeeklyTrend && dailyRaw.size > 7) {
            dailyRaw.chunked(7).map { weekChunk ->
                DailySpendPoint(
                    date = weekChunk.first().date,
                    label = "${weekChunk.first().date.dayOfMonth}",
                    amountPaise = weekChunk.sumOf { it.amountPaise },
                )
            }
        } else if (ChronoUnit.DAYS.between(startDate, endDate) > 60) {
            dailyRaw.chunked(7).map { weekChunk ->
                DailySpendPoint(
                    date = weekChunk.first().date,
                    label = "${weekChunk.first().date.dayOfMonth}",
                    amountPaise = weekChunk.sumOf { it.amountPaise },
                )
            }
        } else {
            dailyRaw
        }

        val catLookup = bundle.categories.associate { it.id to Triple(it.name, it.colorHex, it.iconName) }
        val breakdown = DashboardCalculator.calculateExpenseByCategory(periodTxInputs, catLookup)

        val largest = periodTxEntities
            .filter { it.type == TransactionType.EXPENSE.name }
            .sortedByDescending { it.amountPaise }
            .take(5)

        val activeRecExpenses = bundle.recurring.filter {
            it.isActive && it.type == TransactionType.EXPENSE.name
        }
        val monthlyBurnPaise = activeRecExpenses.sumOf { item ->
            when (runCatching { RecurrenceFrequency.valueOf(item.frequency) }.getOrDefault(RecurrenceFrequency.MONTHLY)) {
                RecurrenceFrequency.DAILY -> item.amountPaise * 30L
                RecurrenceFrequency.WEEKLY -> (item.amountPaise * 52L) / 12L
                RecurrenceFrequency.MONTHLY -> item.amountPaise
                RecurrenceFrequency.YEARLY -> item.amountPaise / 12L
            }
        }

        val budgetResults = bundle.budgets.filter { it.isActive }.map { b ->
            val bPeriod = runCatching { BudgetPeriod.valueOf(b.periodType) }.getOrDefault(BudgetPeriod.MONTHLY)
            val (bStart, bEnd) = DateRangeUtils.budgetPeriodDates(bPeriod, today, b.customStartEpochDay, b.customEndEpochDay)
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
        }

        InsightsUiState(
            isLoading = false,
            filter = filter,
            preferences = prefs,
            summary = summary,
            prevMonthExpensePaise = prevExp,
            currentMonthExpensePaise = currExp,
            momChangePercent = momPct,
            monthlySeries = monthlySeries,
            spendingTrend = trendPoints,
            categoryBreakdown = breakdown,
            largestTransactions = largest,
            estimatedMonthlyRecurringExpensePaise = monthlyBurnPaise,
            activeRecurringCount = activeRecExpenses.size,
            accounts = bundle.accounts.filter { !it.account.isArchived },
            budgets = budgetResults,
            accountMap = bundle.accounts.associate { it.account.id to it.account },
            categoryMap = bundle.categories.associateBy { it.id },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState(),
    )

    fun setPreset(preset: DateRangePreset) {
        filterFlow.update { it.copy(preset = preset) }
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        filterFlow.update {
            it.copy(
                preset = DateRangePreset.CUSTOM,
                customStart = if (start.isAfter(end)) end else start,
                customEnd = if (start.isAfter(end)) start else end,
            )
        }
    }

    fun setShowWeeklyTrend(weekly: Boolean) {
        filterFlow.update { it.copy(showWeeklyTrend = weekly) }
    }

    private data class Bundle(
        val accounts: List<AccountWithBalance>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<com.example.data.local.BudgetEntity>,
        val recurring: List<com.example.data.local.RecurringPaymentEntity>,
    )

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsightsViewModel(financeRepository, preferencesRepository) as T
        }
    }
}
