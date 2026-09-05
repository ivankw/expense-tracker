package com.example.pengeluaran.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "budget_prefs")

class BudgetPreferences(private val context: Context) {

    private val PAYCHECK_KEY = doublePreferencesKey("paycheck_amount")

    // Baca nilai Paycheck
    val paycheckFlow: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[PAYCHECK_KEY] ?: 4000000.0 // Default 4 juta
    }

    // Simpan nilai Paycheck
    suspend fun savePaycheck(amount: Double) {
        context.dataStore.edit { prefs ->
            prefs[PAYCHECK_KEY] = amount
        }
    }

    // Baca Planned Budget per kategori
    fun getPlannedBudgetFlow(category: String, defaultAmount: Double = 0.0): Flow<Double> {
        val key = doublePreferencesKey("planned_$category")
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: defaultAmount
        }
    }

    // Simpan Planned Budget per kategori
    suspend fun savePlannedBudget(category: String, amount: Double) {
        val key = doublePreferencesKey("planned_$category")
        context.dataStore.edit { prefs ->
            prefs[key] = amount
        }
    }
}
