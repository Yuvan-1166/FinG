package com.example.ui.screens.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AccountType
import com.example.domain.model.AppPreferences
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionEntryFormState(
    val editingId: Long? = null,
    val isDetailedMode: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val merchant: String = "",
    val description: String = "",
    val notes: String = "",
    val tags: String = "",
    val receiptUri: String? = null,
    val isDebtIncomingRepayment: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
)

data class TransactionEntryUiState(
    val form: TransactionEntryFormState = TransactionEntryFormState(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
)

data class TransactionDetailUiState(
    val transaction: TransactionEntity? = null,
    val account: AccountEntity? = null,
    val destinationAccount: AccountEntity? = null,
    val category: CategoryEntity? = null,
    val subcategory: CategoryEntity? = null,
    val preferences: AppPreferences = AppPreferences(),
    val isDeleted: Boolean = false,
)

class TransactionViewModel(
    private val financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
    private val transactionId: Long = -1L,
    private val duplicateFromId: Long = -1L,
    private val initialTypeArg: String = "",
    private val initialAccountIdArg: Long = -1L,
) : ViewModel() {

    private val formFlow = MutableStateFlow(TransactionEntryFormState())
    private var initialized = false

    val entryUiState: StateFlow<TransactionEntryUiState> = combine(
        formFlow,
        financeRepository.accountsFlow,
        financeRepository.categoriesFlow,
        preferencesRepository.preferencesFlow,
    ) { form, accounts, categories, prefs ->
        val activeAccounts = accounts.filter { !it.isArchived || it.id == form.accountId || it.id == form.destinationAccountId }
        val resolvedAccountId = form.accountId ?: activeAccounts.firstOrNull()?.id
        val resolvedDestId = if (form.type == TransactionType.TRANSFER) {
            form.destinationAccountId ?: activeAccounts.firstOrNull { it.id != resolvedAccountId }?.id
        } else {
            form.destinationAccountId
        }
        val catType = if (form.type == TransactionType.INCOME) "INCOME" else "EXPENSE"
        val matchingCats = categories.filter { !it.isArchived && it.parentCategoryId == null && it.type == catType }
        val resolvedCatId = when {
            form.type == TransactionType.TRANSFER ||
                form.type == TransactionType.MONEY_LENT ||
                form.type == TransactionType.MONEY_BORROWED ||
                form.type == TransactionType.DEBT_REPAYMENT -> null
            form.categoryId != null && categories.any { it.id == form.categoryId && it.type == catType } -> form.categoryId
            else -> matchingCats.firstOrNull()?.id
        }

        TransactionEntryUiState(
            form = form.copy(
                accountId = resolvedAccountId,
                destinationAccountId = resolvedDestId,
                categoryId = resolvedCatId,
            ),
            accounts = activeAccounts,
            categories = categories.filter { !it.isArchived || it.id == resolvedCatId || it.id == form.subcategoryId },
            preferences = prefs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionEntryUiState(),
    )

    val detailUiState: StateFlow<TransactionDetailUiState> = combine(
        financeRepository.transactionsFlow,
        financeRepository.accountsFlow,
        financeRepository.categoriesFlow,
        preferencesRepository.preferencesFlow,
    ) { txs, accounts, categories, prefs ->
        val tx = txs.firstOrNull { it.id == transactionId }
        val accMap = accounts.associateBy { it.id }
        val catMap = categories.associateBy { it.id }
        TransactionDetailUiState(
            transaction = tx,
            account = tx?.let { accMap[it.accountId] },
            destinationAccount = tx?.destinationAccountId?.let { accMap[it] },
            category = tx?.categoryId?.let { catMap[it] },
            subcategory = tx?.subcategoryId?.let { catMap[it] },
            preferences = prefs,
            isDeleted = initialized && tx == null && transactionId > 0L,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionDetailUiState(),
    )

    init {
        viewModelScope.launch {
            if (transactionId > 0L) {
                financeRepository.getTransactionById(transactionId)?.let { tx ->
                    val parsedType = runCatching { TransactionType.valueOf(tx.type) }
                        .getOrDefault(TransactionType.EXPENSE)
                    formFlow.value = TransactionEntryFormState(
                        editingId = tx.id,
                        isDetailedMode = true,
                        type = parsedType,
                        amountInput = MoneyFormatter.paiseToPlainInput(tx.amountPaise),
                        timestamp = tx.timestamp,
                        accountId = tx.accountId,
                        destinationAccountId = tx.destinationAccountId,
                        categoryId = tx.categoryId,
                        subcategoryId = tx.subcategoryId,
                        merchant = tx.merchant,
                        description = tx.description,
                        notes = tx.notes,
                        tags = tx.tags,
                        receiptUri = tx.receiptUri,
                        isDebtIncomingRepayment = tx.isDebtIncomingRepayment,
                    )
                }
            } else if (duplicateFromId > 0L) {
                financeRepository.getTransactionById(duplicateFromId)?.let { tx ->
                    val parsedType = runCatching { TransactionType.valueOf(tx.type) }
                        .getOrDefault(TransactionType.EXPENSE)
                    formFlow.value = TransactionEntryFormState(
                        editingId = null,
                        isDetailedMode = true,
                        type = parsedType,
                        amountInput = MoneyFormatter.paiseToPlainInput(tx.amountPaise),
                        timestamp = System.currentTimeMillis(),
                        accountId = tx.accountId,
                        destinationAccountId = tx.destinationAccountId,
                        categoryId = tx.categoryId,
                        subcategoryId = tx.subcategoryId,
                        merchant = tx.merchant,
                        description = tx.description,
                        notes = tx.notes,
                        tags = tx.tags,
                        receiptUri = null,
                        isDebtIncomingRepayment = tx.isDebtIncomingRepayment,
                    )
                }
            } else {
                val initType = initialTypeArg.takeIf { it.isNotBlank() }?.let {
                    runCatching { TransactionType.valueOf(it) }.getOrNull()
                } ?: TransactionType.EXPENSE
                val initAcc = initialAccountIdArg.takeIf { it > 0L }
                formFlow.update {
                    it.copy(
                        type = initType,
                        accountId = initAcc,
                        isDetailedMode = initType !in setOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER),
                    )
                }
            }
            initialized = true
        }
    }

    fun setDetailedMode(detailed: Boolean) {
        formFlow.update { it.copy(isDetailedMode = detailed) }
    }

    fun setType(type: TransactionType) {
        formFlow.update {
            it.copy(
                type = type,
                categoryId = null,
                subcategoryId = null,
                errorMessage = null,
            )
        }
    }

    fun setAmountInput(value: String) {
        formFlow.update { it.copy(amountInput = value, errorMessage = null) }
    }

    fun addQuickAmount(rupeesDelta: Long) {
        val currentPaise = MoneyFormatter.parseRupeesToPaise(formFlow.value.amountInput) ?: 0L
        val nextPaise = (currentPaise + rupeesDelta * 100L).coerceAtLeast(0L)
        formFlow.update { it.copy(amountInput = MoneyFormatter.paiseToPlainInput(nextPaise), errorMessage = null) }
    }

    fun setTimestamp(ts: Long) {
        formFlow.update { it.copy(timestamp = ts) }
    }

    fun setAccountId(id: Long) {
        formFlow.update { it.copy(accountId = id, errorMessage = null) }
    }

    fun setDestinationAccountId(id: Long) {
        formFlow.update { it.copy(destinationAccountId = id, errorMessage = null) }
    }

    fun setCategoryId(id: Long) {
        formFlow.update { it.copy(categoryId = id, subcategoryId = null) }
    }

    fun setSubcategoryId(id: Long?) {
        formFlow.update { it.copy(subcategoryId = id) }
    }

    fun setMerchant(value: String) {
        formFlow.update { it.copy(merchant = value) }
    }

    fun setDescription(value: String) {
        formFlow.update { it.copy(description = value) }
    }

    fun setNotes(value: String) {
        formFlow.update { it.copy(notes = value) }
    }

    fun setTags(value: String) {
        formFlow.update { it.copy(tags = value) }
    }

    fun setReceiptUri(uri: String?) {
        formFlow.update { it.copy(receiptUri = uri) }
    }

    fun setDebtIncomingRepayment(incoming: Boolean) {
        formFlow.update { it.copy(isDebtIncomingRepayment = incoming) }
    }

    fun createQuickAccount(name: String, type: AccountType, openingBalanceInput: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val openingPaise = MoneyFormatter.parseRupeesToPaise(openingBalanceInput.ifBlank { "0" }) ?: 0L
        val storedOpening = if (type == AccountType.CREDIT_CARD && openingPaise > 0L) -openingPaise else openingPaise
        viewModelScope.launch {
            val id = financeRepository.saveAccount(
                AccountEntity(
                    name = trimmed,
                    type = type.name,
                    openingBalancePaise = storedOpening,
                    iconName = when (type) {
                        AccountType.CASH -> "wallet"
                        AccountType.UPI -> "upi"
                        AccountType.CREDIT_CARD -> "credit_card"
                        else -> "account_balance"
                    },
                )
            )
            formFlow.update { it.copy(accountId = id, errorMessage = null) }
        }
    }

    fun saveTransaction() {
        val snapshot = entryUiState.value.form
        val amountPaise = MoneyFormatter.parseRupeesToPaise(snapshot.amountInput)
        if (amountPaise == null || amountPaise <= 0L) {
            formFlow.update { it.copy(errorMessage = "Please enter a valid positive amount in INR.") }
            return
        }
        val accountId = snapshot.accountId
        if (accountId == null) {
            formFlow.update { it.copy(errorMessage = "Please select or create an account first.") }
            return
        }
        if (snapshot.type == TransactionType.TRANSFER) {
            val destId = snapshot.destinationAccountId
            if (destId == null || destId == accountId) {
                formFlow.update { it.copy(errorMessage = "Please select a different destination account for a transfer.") }
                return
            }
        }

        formFlow.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val existing = snapshot.editingId?.let { financeRepository.getTransactionById(it) }
                val entity = TransactionEntity(
                    id = snapshot.editingId ?: 0L,
                    type = snapshot.type.name,
                    amountPaise = amountPaise,
                    timestamp = snapshot.timestamp,
                    accountId = accountId,
                    destinationAccountId = if (snapshot.type == TransactionType.TRANSFER) snapshot.destinationAccountId else null,
                    categoryId = if (snapshot.type in setOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.REFUND)) snapshot.categoryId else null,
                    subcategoryId = if (snapshot.type in setOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.REFUND)) snapshot.subcategoryId else null,
                    merchant = snapshot.merchant.trim(),
                    description = snapshot.description.trim(),
                    notes = snapshot.notes.trim(),
                    tags = snapshot.tags.trim(),
                    receiptUri = snapshot.receiptUri,
                    recurringPaymentId = existing?.recurringPaymentId,
                    recurringOccurrenceEpochDay = existing?.recurringOccurrenceEpochDay,
                    debtId = existing?.debtId,
                    isDebtIncomingRepayment = if (snapshot.type == TransactionType.DEBT_REPAYMENT) snapshot.isDebtIncomingRepayment else false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                )
                financeRepository.saveTransaction(entity)
            }.onSuccess {
                formFlow.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { err ->
                formFlow.update {
                    it.copy(isSaving = false, errorMessage = err.message ?: "Unable to save transaction.")
                }
            }
        }
    }

    fun deleteCurrentTransaction() {
        if (transactionId <= 0L) return
        viewModelScope.launch {
            financeRepository.deleteTransaction(transactionId)
        }
    }

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
        private val transactionId: Long = -1L,
        private val duplicateFromId: Long = -1L,
        private val initialType: String = "",
        private val initialAccountId: Long = -1L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionViewModel(
                financeRepository = financeRepository,
                preferencesRepository = preferencesRepository,
                transactionId = transactionId,
                duplicateFromId = duplicateFromId,
                initialTypeArg = initialType,
                initialAccountIdArg = initialAccountId,
            ) as T
        }
    }
}
