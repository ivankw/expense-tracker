package com.example.pengeluaran.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "budget_prefs")

enum class AppThemeMode(val title: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap"),
    AMOLED("Hitam Pekat (AMOLED)")
}

class BudgetPreferences(private val context: Context) {

    companion object {
        val INCOME_KEY = doublePreferencesKey("user_income")
        val THEME_MODE_KEY = stringPreferencesKey("app_theme_mode")
        val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    val incomeFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[INCOME_KEY] ?: 0.0
    }

    suspend fun saveIncome(income: Double) {
        context.dataStore.edit { preferences ->
            preferences[INCOME_KEY] = income
        }
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[THEME_MODE_KEY] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(raw)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED_KEY] ?: false
    }

    suspend fun saveBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }
}
