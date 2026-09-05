package com.example.pengeluaran.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_settings")

class BudgetPreferences(private val context: Context) {

    companion object {
        val BUDGET_KEY = doublePreferencesKey("monthly_budget")
    }

    val budgetFlow: Flow<Double> = context.dataStore.data.map { preferences: Preferences ->
        preferences[BUDGET_KEY] ?: 0.0
    }

    suspend fun saveBudget(budget: Double) {
        context.dataStore.edit { preferences ->
            preferences[BUDGET_KEY] = budget
        }
    }
}
