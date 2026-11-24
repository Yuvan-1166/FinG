package com.example

import android.app.Application
import com.example.data.local.RupeeLedgerDatabase
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.GoldRepository
import com.example.data.repository.StockRepository
import com.example.worker.RecurringReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: RupeeLedgerDatabase by lazy { RupeeLedgerDatabase.create(application) }
    val financeRepository: FinanceRepository by lazy { FinanceRepository(database) }
    val goldRepository: GoldRepository by lazy { GoldRepository(database) }
    val stockRepository: StockRepository by lazy { StockRepository(database) }
    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(application, database.appPreferenceDao())
    }
}

class RupeeLedgerApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.financeRepository.ensureSeeded()
            container.goldRepository.ensureSeeded()
        }
        RecurringReminderWorker.schedule(this)
    }
}
