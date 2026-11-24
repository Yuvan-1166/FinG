package com.example.ui.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.AccountWithBalance
import com.example.data.repository.DeleteResult
import com.example.data.repository.FinanceRepository
import com.example.data.repository.toInput
import com.example.domain.finance.AccountBalanceCalculator
import com.example.domain.finance.DailySpendPoint
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AccountType
import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalLiquidPaise: Long = 0L,
    val totalCreditDuePaise: Long = 0L,
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null,
)

data class AccountDetailUiState(
    val accountWithBalance: AccountWithBalance? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val balanceHistoryPoints: List<DailySpendPoint> = emptyList(),
    val accountMap: Map<Long, AccountEntity> = emptyMap(),
    val categoryMap: Map<Long, CategoryEntity> = emptyMap(),
    val preferences: AppPreferences = AppPreferences(),
)

class AccountsViewModel(
    private val financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
    private val detailAccountId: Long = -1L,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AccountsUiState> = combine(
        financeRepository.accountsWithBalanceFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { accounts, prefs, msg ->
        var liquid = 0L
        var ccDue = 0L
        accounts.filter { !it.account.isArchived }.forEach { item ->
            if (item.account.type == AccountType.CREDIT_CARD.name) {
                if (item.currentBalancePaise < 0L) ccDue += -item.currentBalancePaise
            } else {
                liquid += item.currentBalancePaise
            }
        }
        AccountsUiState(
            accounts = accounts,
            totalLiquidPaise = liquid,
            totalCreditDuePaise = ccDue,
            preferences = prefs,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState(),
    )

    val detailUiState: StateFlow<AccountDetailUiState> = combine(
        financeRepository.accountsWithBalanceFlow,
        financeRepository.transactionsFlow,
        financeRepository.categoriesFlow,
        preferencesRepository.preferencesFlow,
    ) { accounts, txs, categories, prefs ->
        val target = accounts.firstOrNull { it.account.id == detailAccountId }
        val relevant = txs.filter { it.accountId == detailAccountId || it.destinationAccountId == detailAccountId }
        val history = target?.let {
            val rawPoints = AccountBalanceCalculator.calculateAccountHistory(
                account = it.account.toInput(),
                transactions = txs.map { tx -> tx.toInput() },
            )
            val zone = ZoneId.systemDefault()
            rawPoints.map { pt ->
                val d = Instant.ofEpochMilli(pt.timestamp).atZone(zone).toLocalDate()
                DailySpendPoint(date = d, label = "${d.dayOfMonth}", amountPaise = pt.balancePaise)
            }
        } ?: emptyList()

        AccountDetailUiState(
            accountWithBalance = target,
            transactions = relevant,
            balanceHistoryPoints = history,
            accountMap = accounts.associate { it.account.id to it.account },
            categoryMap = categories.associateBy { it.id },
            preferences = prefs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountDetailUiState(),
    )

    fun saveAccount(
        editingId: Long?,
        name: String,
        type: AccountType,
        openingBalanceInput: String,
        institutionName: String,
        colorHex: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            messageFlow.value = "Account name is required."
            return
        }
        val parsed = MoneyFormatter.parseRupeesToPaise(openingBalanceInput.ifBlank { "0" })
        if (parsed == null) {
            messageFlow.value = "Invalid opening balance."
            return
        }
        // For new or edited Credit Card, positive user input of outstanding due is stored as negative liability
        val opening = if (type == AccountType.CREDIT_CARD && parsed > 0L) -parsed else parsed

        viewModelScope.launch {
            val existing = editingId?.let { id -> uiState.value.accounts.firstOrNull { it.account.id == id }?.account }
            financeRepository.saveAccount(
                AccountEntity(
                    id = editingId ?: 0L,
                    name = trimmed,
                    type = type.name,
                    openingBalancePaise = opening,
                    institutionName = institutionName.trim(),
                    colorHex = colorHex,
                    iconName = when (type) {
                        AccountType.CASH -> "wallet"
                        AccountType.UPI -> "upi"
                        AccountType.CREDIT_CARD -> "credit_card"
                        else -> "account_balance"
                    },
                    isArchived = existing?.isArchived ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                )
            )
            messageFlow.value = if (editingId == null) "Account created." else "Account updated."
        }
    }

    fun toggleArchive(account: AccountEntity) {
        viewModelScope.launch {
            financeRepository.toggleAccountArchive(account)
            messageFlow.value = if (account.isArchived) "Account restored." else "Account archived."
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            when (val res = financeRepository.deleteAccount(accountId)) {
                is DeleteResult.Deleted -> messageFlow.value = "Account deleted."
                is DeleteResult.BlockedInUse -> messageFlow.value = res.reason
            }
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
        private val detailAccountId: Long = -1L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountsViewModel(financeRepository, preferencesRepository, detailAccountId) as T
        }
    }
}
