package com.example.ui.screens.gold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldItemEntity
import com.example.data.local.GoldObligationEntity
import com.example.data.local.GoldObligationSettlementEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.GoldRepository
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldObligationDirection
import com.example.domain.gold.GoldObligationStatus
import com.example.domain.gold.GoldPurity
import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoldObligationsUiState(
    val incoming: List<GoldObligationEntity> = emptyList(),
    val outgoing: List<GoldObligationEntity> = emptyList(),
    val settlementsByObligation: Map<Long, List<GoldObligationSettlementEntity>> = emptyMap(),
    val activeInventoryItems: List<GoldItemEntity> = emptyList(),
    val categories: List<GoldCategoryEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val incomingPendingMcg: Long = 0L,
    val outgoingPendingMcg: Long = 0L,
    val statusMessage: String? = null,
)

class GoldObligationsViewModel(
    private val goldRepository: GoldRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    private val baseFlow = combine(
        goldRepository.obligationsFlow,
        goldRepository.settlementsFlow,
        goldRepository.itemsFlow,
        goldRepository.categoriesFlow,
        preferencesRepository.preferencesFlow,
    ) { obs, settlements, items, cats, prefs ->
        ObligationBundle(obs, settlements, items, cats, prefs)
    }

    val uiState: StateFlow<GoldObligationsUiState> = combine(baseFlow, messageFlow) { b, msg ->
        val today = LocalDate.now().toEpochDay()
        val enriched = b.obligations.map { ob ->
            val st = runCatching { GoldObligationStatus.valueOf(ob.status) }.getOrDefault(GoldObligationStatus.OPEN)
            if ((st == GoldObligationStatus.OPEN || st == GoldObligationStatus.PARTIAL) &&
                ob.dueDateEpochDay != null && ob.dueDateEpochDay < today &&
                ob.settledWeightMcg < ob.totalPromisedWeightMcg
            ) {
                ob.copy(status = GoldObligationStatus.OVERDUE.name)
            } else ob
        }

        val incoming = enriched.filter { it.direction == GoldObligationDirection.INCOMING.name }
        val outgoing = enriched.filter { it.direction == GoldObligationDirection.OUTGOING.name }

        fun pendingSum(list: List<GoldObligationEntity>): Long = list
            .filter { it.status != GoldObligationStatus.SETTLED.name && it.status != GoldObligationStatus.CANCELLED.name }
            .sumOf { (it.totalPromisedWeightMcg - it.settledWeightMcg).coerceAtLeast(0L) }

        GoldObligationsUiState(
            incoming = incoming,
            outgoing = outgoing,
            settlementsByObligation = b.settlements.groupBy { it.obligationId },
            activeInventoryItems = b.items.filter {
                it.status != GoldItemStatus.SOLD.name &&
                    it.status != GoldItemStatus.ARCHIVED.name &&
                    it.netWeightMcg > 0L
            },
            categories = b.categories.filter { !it.isArchived && it.parentCategoryId == null },
            preferences = b.prefs,
            incomingPendingMcg = pendingSum(incoming),
            outgoingPendingMcg = pendingSum(outgoing),
            statusMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoldObligationsUiState(),
    )

    fun createObligation(
        direction: GoldObligationDirection,
        counterpartyName: String,
        description: String,
        purity: GoldPurity,
        purityPercentText: String,
        promisedWeightMcg: Long,
        agreedValuePaise: Long,
        dueDateEpochDay: Long?,
        notes: String,
    ) {
        viewModelScope.launch {
            runCatching {
                goldRepository.createObligation(
                    direction = direction,
                    counterpartyName = counterpartyName,
                    description = description,
                    purity = purity,
                    purityPercentText = purityPercentText,
                    promisedWeightMcg = promisedWeightMcg,
                    agreedValuePaise = agreedValuePaise,
                    dueDateEpochDay = dueDateEpochDay,
                    notes = notes,
                )
            }.onSuccess {
                messageFlow.value = "${direction.label} recorded."
            }.onFailure {
                messageFlow.value = it.message ?: "Unable to create obligation."
            }
        }
    }

    fun settleIncoming(
        obligationId: Long,
        receivedWeightMcg: Long,
        convertToInventory: Boolean,
        categoryId: Long?,
        itemTitle: String,
        storageLocation: String,
        notes: String,
    ) {
        viewModelScope.launch {
            runCatching {
                goldRepository.settleIncomingObligation(
                    obligationId = obligationId,
                    receivedWeightMcg = receivedWeightMcg,
                    settlementDateEpochDay = LocalDate.now().toEpochDay(),
                    convertToInventory = convertToInventory,
                    categoryId = categoryId,
                    itemTitle = itemTitle,
                    storageLocation = storageLocation,
                    notes = notes,
                )
            }.onSuccess {
                messageFlow.value = if (convertToInventory) {
                    "Incoming gold received and added to inventory."
                } else {
                    "Incoming gold receipt recorded."
                }
            }.onFailure {
                messageFlow.value = it.message ?: "Failed to settle incoming gold."
            }
        }
    }

    fun settleOutgoing(
        obligationId: Long,
        deliveredWeightMcg: Long,
        sourceGoldItemId: Long,
        notes: String,
    ) {
        viewModelScope.launch {
            runCatching {
                goldRepository.settleOutgoingObligation(
                    obligationId = obligationId,
                    deliveredWeightMcg = deliveredWeightMcg,
                    sourceGoldItemId = sourceGoldItemId,
                    settlementDateEpochDay = LocalDate.now().toEpochDay(),
                    notes = notes,
                )
            }.onSuccess {
                messageFlow.value = "Outgoing gold delivery recorded and inventory updated."
            }.onFailure {
                messageFlow.value = it.message ?: "Failed to record delivery."
            }
        }
    }

    fun cancelObligation(obligation: GoldObligationEntity) {
        viewModelScope.launch {
            goldRepository.cancelObligation(obligation)
            messageFlow.value = "Obligation cancelled."
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    private data class ObligationBundle(
        val obligations: List<GoldObligationEntity>,
        val settlements: List<GoldObligationSettlementEntity>,
        val items: List<GoldItemEntity>,
        val categories: List<GoldCategoryEntity>,
        val prefs: AppPreferences,
    )

    class Factory(
        private val goldRepository: GoldRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoldObligationsViewModel(goldRepository, preferencesRepository) as T
        }
    }
}
