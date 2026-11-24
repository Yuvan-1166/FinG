package com.example.ui.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.GoalContributionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.GoalWithProgress
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalWithProgress> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null,
)

data class GoalDetailUiState(
    val goalWithProgress: GoalWithProgress? = null,
    val contributions: List<GoalContributionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null,
)

class GoalsViewModel(
    private val financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
    private val detailGoalId: Long = -1L,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GoalsUiState> = combine(
        financeRepository.goalsWithProgressFlow,
        financeRepository.accountsFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { goals, accounts, prefs, msg ->
        GoalsUiState(
            goals = goals,
            accounts = accounts.filter { !it.isArchived },
            preferences = prefs,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState(),
    )

    val detailUiState: StateFlow<GoalDetailUiState> = combine(
        financeRepository.goalsWithProgressFlow,
        financeRepository.observeContributionsForGoal(detailGoalId),
        financeRepository.accountsFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { goals, contribs, accounts, prefs, msg ->
        GoalDetailUiState(
            goalWithProgress = goals.firstOrNull { it.goal.id == detailGoalId },
            contributions = contribs,
            accounts = accounts.filter { !it.isArchived },
            preferences = prefs,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalDetailUiState(),
    )

    fun saveGoal(
        editingId: Long?,
        name: String,
        targetInput: String,
        description: String,
        targetDateEpochDay: Long?,
        linkedAccountId: Long?,
        colorHex: String,
    ) {
        val trimmed = name.trim()
        val targetPaise = MoneyFormatter.parseRupeesToPaise(targetInput)
        if (trimmed.isEmpty() || targetPaise == null || targetPaise <= 0L) {
            messageFlow.value = "Enter a valid goal name and positive target amount."
            return
        }
        viewModelScope.launch {
            val existing = editingId?.let { id -> uiState.value.goals.firstOrNull { it.goal.id == id }?.goal }
            financeRepository.saveGoal(
                SavingsGoalEntity(
                    id = editingId ?: 0L,
                    name = trimmed,
                    targetAmountPaise = targetPaise,
                    targetDateEpochDay = targetDateEpochDay,
                    description = description.trim(),
                    iconName = "savings",
                    colorHex = colorHex,
                    linkedAccountId = linkedAccountId,
                    isCompleted = existing?.isCompleted ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                )
            )
            messageFlow.value = if (editingId == null) "Savings goal created." else "Goal updated."
        }
    }

    fun contributeOrWithdraw(
        goal: SavingsGoalEntity,
        amountInput: String,
        isWithdrawal: Boolean,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        note: String,
    ) {
        val paise = MoneyFormatter.parseRupeesToPaise(amountInput)
        if (paise == null || paise <= 0L) {
            messageFlow.value = "Enter a valid positive amount."
            return
        }
        val currentSaved = detailUiState.value.goalWithProgress?.savedAmountPaise ?: 0L
        if (isWithdrawal && paise > currentSaved) {
            messageFlow.value = "Cannot withdraw more than current saved amount."
            return
        }
        val signedPaise = if (isWithdrawal) -paise else paise
        viewModelScope.launch {
            financeRepository.addGoalContribution(
                goal = goal,
                amountPaise = signedPaise,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                note = note.trim(),
            )
            messageFlow.value = if (isWithdrawal) "Withdrawal recorded." else "Contribution added."
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            financeRepository.deleteGoal(goalId)
            messageFlow.value = "Goal deleted."
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    class Factory(
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
        private val detailGoalId: Long = -1L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalsViewModel(financeRepository, preferencesRepository, detailGoalId) as T
        }
    }
}
