package com.example.ui.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.toInput
import com.example.domain.finance.BudgetUtilizationResult
import com.example.domain.finance.DashboardCalculator
import com.example.domain.finance.DateRangeUtils
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AppPreferences
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class BudgetHistoryEntry(
    val periodLabel: String,
    val spentPaise: Long,
    val limitPaise: Long,
    val percent: Int,
)

data class BudgetDisplayItem(
    val entity: BudgetEntity,
    val utilization: BudgetUtilizationResult,
    val categoryName: String,
    val history: List<BudgetHistoryEntry>,
)

data class BudgetsUiState(
    val items: List<BudgetDisplayItem> = emptyList(),
    val alerts: List<BudgetUtilizationResult> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null,
)

class BudgetsViewModel(
    private val financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetsUiState> = combine(
        financeRepository.budgetsFlow,
        financeRepository.categoriesFlow,
        financeRepository.transactionsFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { budgets, categories, transactions, prefs, msg ->
        val catMap = categories.associateBy { it.id }
        val txInputs = transactions.map { it.toInput() }
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)

        val displayItems = budgets.map { b ->
            val period = runCatching { BudgetPeriod.valueOf(b.periodType) }.getOrDefault(BudgetPeriod.MONTHLY)
            val (startD, endD) = DateRangeUtils.budgetPeriodDates(period, today, b.customStartEpochDay, b.customEndEpochDay)
            val (startMs, endMs) = DateRangeUtils.toEpochMillisRange(startD, endD)
            val periodTx = txInputs.filter { it.timestamp in startMs..endMs }
            val util = DashboardCalculator.calculateBudgetUtilization(
                budgetId = b.id,
                name = b.name,
                categoryId = b.categoryId,
                limitPaise = b.limitAmountPaise,
                alertThresholdPercent = b.alertThresholdPercent,
                periodTransactions = periodTx,
            )

            // Past 3 months history for monthly/category budgets
            val history = (1L..3L).map { offset ->
                val ym = currentMonth.minusMonths(offset)
                val (hStart, hEnd) = DateRangeUtils.monthRange(ym)
                val hTx = txInputs.filter { it.timestamp in hStart..hEnd }
                val hUtil = DashboardCalculator.calculateBudgetUtilization(
                    budgetId = b.id,
                    name = b.name,
                    categoryId = b.categoryId,
                    limitPaise = b.limitAmountPaise,
                    alertThresholdPercent = b.alertThresholdPercent,
                    periodTransactions = hTx,
                )
                BudgetHistoryEntry(
                    periodLabel = ym.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    spentPaise = hUtil.spentPaise,
                    limitPaise = b.limitAmountPaise,
                    percent = hUtil.utilizationPercent,
                )
            }

            BudgetDisplayItem(
                entity = b,
                utilization = util,
                categoryName = b.categoryId?.let { catMap[it]?.name } ?: "All Expenses (Overall)",
                history = history,
            )
        }

        BudgetsUiState(
            items = displayItems,
            alerts = displayItems.filter { it.entity.isActive && it.utilization.status != BudgetStatus.NORMAL }
                .map { it.utilization },
            expenseCategories = categories.filter { !it.isArchived && it.type == "EXPENSE" && it.parentCategoryId == null },
            preferences = prefs,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState(),
    )

    fun saveBudget(
        editingId: Long?,
        name: String,
        categoryId: Long?,
        limitInput: String,
        period: BudgetPeriod,
        customStartEpochDay: Long?,
        customEndEpochDay: Long?,
        alertThresholdPercent: Int,
    ) {
        val limitPaise = MoneyFormatter.parseRupeesToPaise(limitInput)
        if (limitPaise == null || limitPaise <= 0L) {
            messageFlow.value = "Enter a valid positive budget limit."
            return
        }
        val cleanName = name.trim().ifEmpty {
            if (categoryId == null) "${period.label} Overall Budget" else "Category Budget"
        }
        viewModelScope.launch {
            financeRepository.saveBudget(
                BudgetEntity(
                    id = editingId ?: 0L,
                    name = cleanName,
                    categoryId = categoryId,
                    limitAmountPaise = limitPaise,
                    periodType = period.name,
                    customStartEpochDay = if (period == BudgetPeriod.CUSTOM) customStartEpochDay else null,
                    customEndEpochDay = if (period == BudgetPeriod.CUSTOM) customEndEpochDay else null,
                    alertThresholdPercent = alertThresholdPercent.coerceIn(50, 99),
                    isActive = true,
                )
            )
            messageFlow.value = if (editingId == null) "Budget created." else "Budget updated."
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            financeRepository.deleteBudget(id)
            messageFlow.value = "Budget removed."
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetsViewModel(financeRepository, preferencesRepository) as T
        }
    }
}
