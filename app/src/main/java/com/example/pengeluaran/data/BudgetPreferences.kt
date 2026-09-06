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
        val INCOME_KEY = doublePreferencesKey("monthly_income")
        val BUDGET_KEY = doublePreferencesKey("monthly_budget")
    }

    val incomeFlow: Flow<Double> = context.dataStore.data.map { preferences: Preferences ->
        preferences[INCOME_KEY] ?: 0.0
    }

    val budgetFlow: Flow<Double> = context.dataStore.data.map { preferences: Preferences ->
        preferences[BUDGET_KEY] ?: 0.0
    }

    suspend fun saveIncome(income: Double) {
        context.dataStore.edit { preferences ->
            preferences[INCOME_KEY] = income
        }
    }

    suspend fun saveBudget(budget: Double) {
        context.dataStore.edit { preferences ->
            preferences[BUDGET_KEY] = budget
        }
    }
}
