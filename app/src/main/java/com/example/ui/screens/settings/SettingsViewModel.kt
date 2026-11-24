package com.example.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.CsvImportAnalysis
import com.example.data.backup.DataTransferService
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.DatabaseSnapshot
import com.example.data.repository.FinanceRepository
import com.example.domain.model.AppPreferences
import com.example.domain.model.CurrencyDisplayMode
import com.example.domain.model.DateFormatPreference
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val pendingCsvImport: CsvImportAnalysis? = null,
    val pendingBackupRestore: DatabaseSnapshot? = null,
    val statusMessage: String? = null,
)

class SettingsViewModel(
    private val financeRepository: FinanceRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val csvImportFlow = MutableStateFlow<CsvImportAnalysis?>(null)
    private val backupRestoreFlow = MutableStateFlow<DatabaseSnapshot?>(null)
    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferencesFlow,
        csvImportFlow,
        backupRestoreFlow,
        messageFlow,
    ) { prefs, csv, backup, msg ->
        SettingsUiState(
            preferences = prefs,
            pendingCsvImport = csv,
            pendingBackupRestore = backup,
            statusMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setCurrencyDisplayMode(mode: CurrencyDisplayMode) {
        viewModelScope.launch { preferencesRepository.setCurrencyDisplayMode(mode) }
    }

    fun setShowPaiseDecimals(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowPaiseDecimals(show) }
    }

    fun setDateFormat(format: DateFormatPreference) {
        viewModelScope.launch { preferencesRepository.setDateFormat(format) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBudgetAlertsEnabled(enabled) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAppLockEnabled(enabled) }
    }

    fun exportCsvToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val txs = financeRepository.transactionsFlow.first()
                val accMap = financeRepository.accountsFlow.first().associateBy { it.id }
                val catMap = financeRepository.categoriesFlow.first().associateBy { it.id }
                val csv = DataTransferService.exportTransactionsToCsv(txs, accMap, catMap)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output stream.")
                }
                txs.size
            }.onSuccess { count ->
                messageFlow.value = "Exported $count transactions to CSV."
            }.onFailure { err ->
                messageFlow.value = "CSV export failed: ${err.message}"
            }
        }
    }

    fun prepareCsvImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Unable to read selected file.")
                }
                val existing = financeRepository.transactionsFlow.first()
                val accMap = financeRepository.accountsFlow.first().associateBy { it.id }
                DataTransferService.analyzeCsvImport(raw, existing, accMap)
            }.onSuccess { analysis ->
                csvImportFlow.value = analysis
            }.onFailure { err ->
                messageFlow.value = "CSV import error: ${err.message}"
            }
        }
    }

    fun confirmCsvImport() {
        val analysis = csvImportFlow.value ?: return
        csvImportFlow.value = null
        if (analysis.validRows.isEmpty()) {
            messageFlow.value = "No new valid rows to import."
            return
        }
        viewModelScope.launch {
            runCatching {
                val accounts = financeRepository.accountsFlow.first().associateBy { it.name.lowercase() }.toMutableMap()
                val categories = financeRepository.categoriesFlow.first().associateBy { it.name.lowercase() }.toMutableMap()

                suspend fun resolveAccountId(name: String): Long {
                    val key = name.lowercase()
                    accounts[key]?.let { return it.id }
                    val newAcc = AccountEntity(
                        name = name,
                        type = DataTransferService.inferAccountTypeFromName(name).name,
                        openingBalancePaise = 0L,
                    )
                    val id = financeRepository.saveAccount(newAcc)
                    accounts[key] = newAcc.copy(id = id)
                    return id
                }

                suspend fun resolveCategoryId(name: String?, type: String): Long? {
                    val clean = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                    val key = clean.lowercase()
                    categories[key]?.let { return it.id }
                    val newCat = CategoryEntity(
                        name = clean,
                        type = if (type == "INCOME") "INCOME" else "EXPENSE",
                    )
                    val id = financeRepository.saveCategory(newCat)
                    categories[key] = newCat.copy(id = id)
                    return id
                }

                var imported = 0
                for (row in analysis.validRows) {
                    val accId = resolveAccountId(row.accountName)
                    val destId = row.destinationAccountName?.let { resolveAccountId(it) }
                    val catId = resolveCategoryId(row.categoryName, row.type.name)
                    financeRepository.saveTransaction(
                        TransactionEntity(
                            type = row.type.name,
                            amountPaise = row.amountPaise,
                            timestamp = row.timestamp,
                            accountId = accId,
                            destinationAccountId = destId,
                            categoryId = catId,
                            merchant = row.merchant,
                            description = row.description,
                            notes = row.notes,
                            tags = row.tags,
                        )
                    )
                    imported++
                }
                imported
            }.onSuccess { imported ->
                messageFlow.value = "Imported $imported transaction(s) (${analysis.duplicateRows.size} duplicate(s) skipped)."
            }.onFailure { err ->
                messageFlow.value = "Import failed: ${err.message}"
            }
        }
    }

    fun dismissCsvImport() {
        csvImportFlow.value = null
    }

    fun exportJsonBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val snapshot = financeRepository.getSnapshot()
                val jsonStr = DataTransferService.exportSnapshotToJson(snapshot)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open backup file.")
                }
            }.onSuccess {
                messageFlow.value = "Local database backup saved."
            }.onFailure { err ->
                messageFlow.value = "Backup failed: ${err.message}"
            }
        }
    }

    fun prepareJsonRestore(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Unable to read backup file.")
                }
                DataTransferService.parseSnapshotFromJson(raw).getOrThrow()
            }.onSuccess { snapshot ->
                backupRestoreFlow.value = snapshot
            }.onFailure { err ->
                messageFlow.value = "Invalid backup file: ${err.message}"
            }
        }
    }

    fun confirmJsonRestore() {
        val snapshot = backupRestoreFlow.value ?: return
        backupRestoreFlow.value = null
        viewModelScope.launch {
            runCatching {
                financeRepository.restoreSnapshot(snapshot)
            }.onSuccess {
                messageFlow.value = "Database restored (${snapshot.transactions.size} transactions)."
            }.onFailure { err ->
                messageFlow.value = "Restore failed: ${err.message}"
            }
        }
    }

    fun dismissJsonRestore() {
        backupRestoreFlow.value = null
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
            return SettingsViewModel(financeRepository, preferencesRepository) as T
        }
    }
}
