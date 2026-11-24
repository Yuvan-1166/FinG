package com.example.ui.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AppPreferences
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringUiState(
    val items: List<RecurringPaymentEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accountMap: Map<Long, AccountEntity> = emptyMap(),
    val categoryMap: Map<Long, CategoryEntity> = emptyMap(),
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null,
)

class RecurringViewModel(
    private val financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecurringUiState> = combine(
        financeRepository.recurringPaymentsFlow,
        financeRepository.accountsFlow,
        financeRepository.categoriesFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { items, accounts, categories, prefs, msg ->
        RecurringUiState(
            items = items,
            accounts = accounts.filter { !it.isArchived },
            categories = categories.filter { !it.isArchived && it.parentCategoryId == null },
            accountMap = accounts.associateBy { it.id },
            categoryMap = categories.associateBy { it.id },
            preferences = prefs,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState(),
    )

    fun saveRecurring(
        editingId: Long?,
        title: String,
        type: TransactionType,
        amountInput: String,
        frequency: RecurrenceFrequency,
        startEpochDay: Long,
        endEpochDay: Long?,
        accountId: Long?,
        categoryId: Long?,
        merchant: String,
        notes: String,
        reminderEnabled: Boolean,
        reminderDaysBefore: Int,
    ) {
        val cleanTitle = title.trim()
        val amountPaise = MoneyFormatter.parseRupeesToPaise(amountInput)
        if (cleanTitle.isEmpty() || amountPaise == null || amountPaise <= 0L || accountId == null) {
            messageFlow.value = "Enter title, valid positive amount, and account."
            return
        }
        viewModelScope.launch {
            val existing = editingId?.let { id -> uiState.value.items.firstOrNull { it.id == id } }
            financeRepository.saveRecurringPayment(
                RecurringPaymentEntity(
                    id = editingId ?: 0L,
                    title = cleanTitle,
                    type = if (type == TransactionType.INCOME) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                    amountPaise = amountPaise,
                    frequency = frequency.name,
                    startDateEpochDay = startEpochDay,
                    endDateEpochDay = endEpochDay,
                    nextDueDateEpochDay = existing?.nextDueDateEpochDay ?: startEpochDay,
                    lastConfirmedEpochDay = existing?.lastConfirmedEpochDay,
                    accountId = accountId,
                    categoryId = categoryId,
                    merchant = merchant.trim(),
                    notes = notes.trim(),
                    reminderEnabled = reminderEnabled,
                    reminderDaysBefore = reminderDaysBefore.coerceIn(0, 14),
                    isActive = existing?.isActive ?: true,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                )
            )
            messageFlow.value = if (editingId == null) "Recurring payment scheduled." else "Recurring payment updated."
        }
    }

    fun confirmOccurrence(item: RecurringPaymentEntity) {
        viewModelScope.launch {
            val ok = financeRepository.confirmRecurringOccurrence(item)
            messageFlow.value = if (ok) "Payment logged & next date updated." else "Already logged for this due date."
        }
    }

    fun skipOccurrence(item: RecurringPaymentEntity) {
        viewModelScope.launch {
            financeRepository.skipRecurringOccurrence(item)
            messageFlow.value = "Skipped occurrence."
        }
    }

    fun toggleActive(item: RecurringPaymentEntity) {
        viewModelScope.launch {
            financeRepository.toggleRecurringActive(item)
            messageFlow.value = if (item.isActive) "Paused." else "Resumed."
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            financeRepository.deleteRecurringPayment(id)
            messageFlow.value = "Recurring item deleted."
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
            return RecurringViewModel(financeRepository, preferencesRepository) as T
        }
    }
}
