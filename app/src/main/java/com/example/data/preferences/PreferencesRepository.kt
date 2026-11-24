package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AppPreferenceDao
import com.example.data.local.AppPreferenceEntity
import com.example.domain.model.AppPreferences
import com.example.domain.model.CurrencyDisplayMode
import com.example.domain.model.DateFormatPreference
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rupee_ledger_prefs")

class PreferencesRepository(
    private val context: Context,
    private val appPreferenceDao: AppPreferenceDao,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_DISPLAY = stringPreferencesKey("currency_display")
        val SHOW_PAISE = booleanPreferencesKey("show_paise")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            currencyDisplayMode = prefs[Keys.CURRENCY_DISPLAY]?.let {
                runCatching { CurrencyDisplayMode.valueOf(it) }.getOrNull()
            } ?: CurrencyDisplayMode.STANDARD_INR,
            showPaiseDecimals = prefs[Keys.SHOW_PAISE] ?: true,
            dateFormat = prefs[Keys.DATE_FORMAT]?.let {
                runCatching { DateFormatPreference.valueOf(it) }.getOrNull()
            } ?: DateFormatPreference.DD_MMM_YYYY,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        updateKey(Keys.THEME_MODE, mode.name)
    }

    suspend fun setCurrencyDisplayMode(mode: CurrencyDisplayMode) {
        updateKey(Keys.CURRENCY_DISPLAY, mode.name)
    }

    suspend fun setShowPaiseDecimals(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PAISE] = show }
        appPreferenceDao.upsert(AppPreferenceEntity(Keys.SHOW_PAISE.name, show.toString()))
    }

    suspend fun setDateFormat(format: DateFormatPreference) {
        updateKey(Keys.DATE_FORMAT, format.name)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
        appPreferenceDao.upsert(AppPreferenceEntity(Keys.NOTIFICATIONS_ENABLED.name, enabled.toString()))
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BUDGET_ALERTS_ENABLED] = enabled }
        appPreferenceDao.upsert(AppPreferenceEntity(Keys.BUDGET_ALERTS_ENABLED.name, enabled.toString()))
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
        appPreferenceDao.upsert(AppPreferenceEntity(Keys.APP_LOCK_ENABLED.name, enabled.toString()))
    }

    private suspend fun updateKey(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
        appPreferenceDao.upsert(AppPreferenceEntity(key.name, value))
    }
}
