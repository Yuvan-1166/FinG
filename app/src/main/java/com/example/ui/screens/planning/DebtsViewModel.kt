package com.example.ui.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.DebtRepaymentEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.DebtWithBalance
import com.example.data.repository.FinanceRepository
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AppPreferences
import com.example.domain.model.DebtDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtsUiState(
    val debts: List<DebtWithBalance> = emptyList(),
    val totalReceivablePaise: Long = 0L,
    val totalPayablePaise: Long = 0L,
    val accounts: List<AccountEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null,
)

data class DebtDetailUiState(
    val debtWithBalance: DebtWithBalance? = null,
    val repayments: List<DebtRepaymentEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null,
)

class DebtsViewModel(
    private val financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
    private val detailDebtId: Long = -1L,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DebtsUiState> = combine(
        financeRepository.debtsWithBalanceFlow,
        financeRepository.accountsFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { debts, accounts, prefs, msg ->
        val receivable = debts
            .filter { !it.debt.isSettled && it.debt.direction == DebtDirection.LENT.name }
            .sumOf { it.outstandingAmountPaise }
        val payable = debts
            .filter { !it.debt.isSettled && it.debt.direction == DebtDirection.BORROWED.name }
            .sumOf { it.outstandingAmountPaise }
        DebtsUiState(
            debts = debts,
            totalReceivablePaise = receivable,
            totalPayablePaise = payable,
            accounts = accounts.filter { !it.isArchived },
            preferences = prefs,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtsUiState(),
    )

    val detailUiState: StateFlow<DebtDetailUiState> = combine(
        financeRepository.debtsWithBalanceFlow,
        financeRepository.observeRepaymentsForDebt(detailDebtId),
        financeRepository.accountsFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { debts, repayments, accounts, prefs, msg ->
        DebtDetailUiState(
            debtWithBalance = debts.firstOrNull { it.debt.id == detailDebtId },
            repayments = repayments,
            accounts = accounts.filter { !it.isArchived },
            preferences = prefs,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtDetailUiState(),
    )

    fun createDebt(
        personName: String,
        direction: DebtDirection,
        amountInput: String,
        accountId: Long?,
        logAccountTransaction: Boolean,
        dueDateEpochDay: Long?,
        notes: String,
    ) {
        val cleanName = personName.trim()
        val amountPaise = MoneyFormatter.parseRupeesToPaise(amountInput)
        if (cleanName.isEmpty() || amountPaise == null || amountPaise <= 0L) {
            messageFlow.value = "Enter person's name and positive amount."
            return
        }
        viewModelScope.launch {
            financeRepository.createDebt(
                personName = cleanName,
                direction = direction,
                principalAmountPaise = amountPaise,
                accountId = accountId,
                logAccountTransaction = logAccountTransaction,
                dueDateEpochDay = dueDateEpochDay,
                notes = notes.trim(),
            )
            messageFlow.value = "Debt record saved."
        }
    }

    fun recordRepayment(
        item: DebtWithBalance,
        amountInput: String,
        accountId: Long?,
        logAccountTransaction: Boolean,
        note: String,
    ) {
        val paise = MoneyFormatter.parseRupeesToPaise(amountInput)
        if (paise == null || paise <= 0L) {
            messageFlow.value = "Enter a valid positive repayment amount."
            return
        }
        if (paise > item.outstandingAmountPaise) {
            messageFlow.value = "Repayment cannot exceed outstanding balance."
            return
        }
        viewModelScope.launch {
            financeRepository.addDebtRepayment(
                debt = item.debt,
                amountPaise = paise,
                accountId = accountId,
                logAccountTransaction = logAccountTransaction,
                note = note.trim(),
            )
            messageFlow.value = "Repayment recorded."
        }
    }

    fun deleteDebt(id: Long) {
        viewModelScope.launch {
            financeRepository.deleteDebt(id)
            messageFlow.value = "Debt deleted."
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
        private val detailDebtId: Long = -1L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DebtsViewModel(financeRepository, preferencesRepository, detailDebtId) as T
        }
    }
}
