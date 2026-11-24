package com.example.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.finance.DateRangeUtils
import com.example.domain.model.AppPreferences
import com.example.domain.model.DateRangePreset
import com.example.domain.model.TransactionSortOrder
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class ActivityFilter(
    val searchQuery: String = "",
    val datePreset: DateRangePreset? = null, // null = All time
    val customStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val customEndDate: LocalDate = LocalDate.now(),
    val selectedType: TransactionType? = null,
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
)

data class ActivityUiState(
    val isLoading: Boolean = true,
    val filter: ActivityFilter = ActivityFilter(),
    val preferences: AppPreferences = AppPreferences(),
    val transactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accountMap: Map<Long, AccountEntity> = emptyMap(),
    val categoryMap: Map<Long, CategoryEntity> = emptyMap(),
    val filteredInflowPaise: Long = 0L,
    val filteredOutflowPaise: Long = 0L,
)

class ActivityViewModel(
    financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(ActivityFilter())

    val uiState: StateFlow<ActivityUiState> = combine(
        financeRepository.transactionsFlow,
        financeRepository.accountsFlow,
        financeRepository.categoriesFlow,
        preferencesRepository.preferencesFlow,
        filterFlow,
    ) { transactions, accounts, categories, prefs, filter ->
        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        val dateRange = filter.datePreset?.let { preset ->
            val (s, e) = DateRangeUtils.presetDates(
                preset = preset,
                customStart = filter.customStartDate,
                customEnd = filter.customEndDate,
            )
            DateRangeUtils.toEpochMillisRange(s, e)
        }

        val q = filter.searchQuery.trim().lowercase()
        val filtered = transactions.filter { tx ->
            if (dateRange != null && tx.timestamp !in dateRange.first..dateRange.second) return@filter false
            if (filter.selectedType != null && tx.type != filter.selectedType.name) return@filter false
            if (filter.selectedAccountId != null &&
                tx.accountId != filter.selectedAccountId &&
                tx.destinationAccountId != filter.selectedAccountId
            ) return@filter false
            if (filter.selectedCategoryId != null &&
                tx.categoryId != filter.selectedCategoryId &&
                tx.subcategoryId != filter.selectedCategoryId
            ) return@filter false

            if (q.isNotEmpty()) {
                val accName = accountMap[tx.accountId]?.name?.lowercase() ?: ""
                val destName = tx.destinationAccountId?.let { accountMap[it]?.name?.lowercase() } ?: ""
                val catName = tx.categoryId?.let { categoryMap[it]?.name?.lowercase() } ?: ""
                val matches = tx.merchant.lowercase().contains(q) ||
                    tx.description.lowercase().contains(q) ||
                    tx.notes.lowercase().contains(q) ||
                    tx.tags.lowercase().contains(q) ||
                    accName.contains(q) ||
                    destName.contains(q) ||
                    catName.contains(q)
                if (!matches) return@filter false
            }
            true
        }

        val sorted = when (filter.sortOrder) {
            TransactionSortOrder.DATE_DESC -> filtered.sortedWith(compareByDescending<TransactionEntity> { it.timestamp }.thenByDescending { it.id })
            TransactionSortOrder.DATE_ASC -> filtered.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
            TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amountPaise }
            TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amountPaise }
        }

        var inflow = 0L
        var outflow = 0L
        for (tx in sorted) {
            when (tx.type) {
                TransactionType.INCOME.name, TransactionType.REFUND.name -> inflow += tx.amountPaise
                TransactionType.EXPENSE.name -> outflow += tx.amountPaise
            }
        }

        ActivityUiState(
            isLoading = false,
            filter = filter,
            preferences = prefs,
            transactions = sorted,
            accounts = accounts,
            categories = categories,
            accountMap = accountMap,
            categoryMap = categoryMap,
            filteredInflowPaise = inflow,
            filteredOutflowPaise = outflow,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActivityUiState(),
    )

    fun setSearchQuery(query: String) {
        filterFlow.update { it.copy(searchQuery = query) }
    }

    fun setDatePreset(preset: DateRangePreset?) {
        filterFlow.update { it.copy(datePreset = preset) }
    }

    fun setCustomDates(start: LocalDate, end: LocalDate) {
        filterFlow.update {
            it.copy(
                datePreset = DateRangePreset.CUSTOM,
                customStartDate = if (start.isAfter(end)) end else start,
                customEndDate = if (start.isAfter(end)) start else end,
            )
        }
    }

    fun setTypeFilter(type: TransactionType?) {
        filterFlow.update { it.copy(selectedType = type) }
    }

    fun setAccountFilter(accountId: Long?) {
        filterFlow.update { it.copy(selectedAccountId = accountId) }
    }

    fun setCategoryFilter(categoryId: Long?) {
        filterFlow.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setSortOrder(order: TransactionSortOrder) {
        filterFlow.update { it.copy(sortOrder = order) }
    }

    fun clearFilters() {
        filterFlow.value = ActivityFilter()
    }

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActivityViewModel(financeRepository, preferencesRepository) as T
        }
    }
}
