package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(private val context: Context) {

    companion object {
        val KEY_PRIMARY_CURRENCY = stringPreferencesKey("primary_currency")
        val KEY_APP_THEME = stringPreferencesKey("app_theme") // "DARK", "LIGHT", "SYSTEM"
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color") // Hex string
        val KEY_WEEK_START_DAY = stringPreferencesKey("week_start_day") // "SATURDAY", "SUNDAY", "MONDAY"
        val KEY_SECURITY_PIN = stringPreferencesKey("security_pin") // 4 or 6 digits or empty
        val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val KEY_STREAK_COUNT = intPreferencesKey("streak_count")
        val KEY_LAST_STREAK_DATE = longPreferencesKey("last_streak_date")
        val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
    }

    val primaryCurrency: Flow<String> = context.dataStore.data.map { it[KEY_PRIMARY_CURRENCY] ?: "BDT" }
    val appTheme: Flow<String> = context.dataStore.data.map { it[KEY_APP_THEME] ?: "DARK" }
    val accentColor: Flow<String> = context.dataStore.data.map { it[KEY_ACCENT_COLOR] ?: "#6C63FF" }
    val weekStartDay: Flow<String> = context.dataStore.data.map { it[KEY_WEEK_START_DAY] ?: "SUNDAY" }
    val securityPin: Flow<String> = context.dataStore.data.map { it[KEY_SECURITY_PIN] ?: "" }
    val pinEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_PIN_ENABLED] ?: false }
    val streakCount: Flow<Int> = context.dataStore.data.map { it[KEY_STREAK_COUNT] ?: 1 }
    val lastStreakDate: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_STREAK_DATE] ?: 0L }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATION_ENABLED] ?: true }
    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }

    suspend fun setPrimaryCurrency(currency: String) {
        context.dataStore.edit { it[KEY_PRIMARY_CURRENCY] = currency }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[KEY_APP_THEME] = theme }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { it[KEY_ACCENT_COLOR] = color }
    }

    suspend fun setWeekStartDay(day: String) {
        context.dataStore.edit { it[KEY_WEEK_START_DAY] = day }
    }

    suspend fun setSecurityPin(pin: String) {
        context.dataStore.edit {
            it[KEY_SECURITY_PIN] = pin
            it[KEY_PIN_ENABLED] = pin.isNotEmpty()
        }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PIN_ENABLED] = enabled }
    }

    suspend fun updateStreak() {
        val today = System.currentTimeMillis()
        context.dataStore.edit { preferences ->
            val lastDate = preferences[KEY_LAST_STREAK_DATE] ?: 0L
            val currentStreak = preferences[KEY_STREAK_COUNT] ?: 0
            
            val diff = today - lastDate
            val oneDayMillis = 24 * 60 * 60 * 1000L
            
            if (diff in oneDayMillis..(oneDayMillis * 2)) {
                preferences[KEY_STREAK_COUNT] = currentStreak + 1
            } else if (diff > oneDayMillis * 2) {
                preferences[KEY_STREAK_COUNT] = 1
            }
            preferences[KEY_LAST_STREAK_DATE] = today
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }
}
