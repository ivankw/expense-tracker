package com.example.pengeluaran.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "budget_prefs")

class BudgetPreferences(private val context: Context) {
    companion object {
        val KEY_INCOME = doublePreferencesKey("user_income")
    }

    val incomeFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_INCOME] ?: 4750000.0 // Default pemasukan jika belum pernah diedit
    }

    suspend fun saveIncome(income: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_INCOME] = income
        }
    }
}
