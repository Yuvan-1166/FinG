package com.example.ui.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CategoryEntity
import com.example.data.repository.DeleteResult
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val selectedType: String = "EXPENSE",
    val message: String? = null,
)

class CategoriesViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val typeFlow = MutableStateFlow("EXPENSE")
    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        financeRepository.categoriesFlow,
        typeFlow,
        messageFlow,
    ) { cats, type, msg ->
        CategoriesUiState(
            categories = cats.filter { it.type == type },
            selectedType = type,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(),
    )

    fun setSelectedType(type: String) {
        typeFlow.value = type
    }

    fun saveCategory(
        editingId: Long?,
        name: String,
        type: String,
        parentCategoryId: Long?,
        iconName: String,
        colorHex: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            messageFlow.value = "Category name is required."
            return
        }
        viewModelScope.launch {
            val existing = editingId?.let { id -> uiState.value.categories.firstOrNull { it.id == id } }
            financeRepository.saveCategory(
                CategoryEntity(
                    id = editingId ?: 0L,
                    name = trimmed,
                    type = type,
                    parentCategoryId = parentCategoryId,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = existing?.isDefault ?: false,
                    isArchived = existing?.isArchived ?: false,
                )
            )
            messageFlow.value = if (editingId == null) "Category saved." else "Category updated."
        }
    }

    fun toggleArchive(cat: CategoryEntity) {
        viewModelScope.launch {
            financeRepository.toggleCategoryArchive(cat)
            messageFlow.value = if (cat.isArchived) "Category restored." else "Category archived."
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            when (val res = financeRepository.deleteCategory(categoryId)) {
                is DeleteResult.Deleted -> messageFlow.value = "Category deleted."
                is DeleteResult.BlockedInUse -> messageFlow.value = res.reason
            }
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    class Factory(private val financeRepository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(financeRepository) as T
        }
    }
}
