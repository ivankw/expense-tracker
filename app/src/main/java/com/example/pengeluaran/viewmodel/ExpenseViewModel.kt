package com.example.pengeluaran.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pengeluaran.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val expenseDao = db.expenseDao()
    private val recurringDao = db.recurringDao()
    private val categoryBudgetDao = db.categoryBudgetDao()
    private val budgetPreferences = BudgetPreferences(application)

    val expenses: StateFlow<List<Expense>> = expenseDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringBills: StateFlow<List<RecurringBill>> = recurringDao.getAllRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBudgets: StateFlow<List<CategoryBudget>> = categoryBudgetDao.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeFlow: StateFlow<Double> = budgetPreferences.incomeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val themeModeFlow: StateFlow<AppThemeMode> = budgetPreferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val biometricEnabledFlow: StateFlow<Boolean> = budgetPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            budgetPreferences.saveBiometricEnabled(enabled)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            budgetPreferences.saveThemeMode(mode)
        }
    }

    fun addExpense(title: String, amount: Double, category: String, dateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            expenseDao.insertExpense(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    date = dateMillis
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
        }
    }

    fun saveIncome(income: Double) {
        viewModelScope.launch {
            budgetPreferences.saveIncome(income)
        }
    }

    fun setCategoryBudget(category: String, limit: Double) {
        viewModelScope.launch {
            if (limit <= 0) {
                categoryBudgetDao.deleteBudget(CategoryBudget(category, limit))
            } else {
                categoryBudgetDao.setBudget(CategoryBudget(category, limit))
            }
        }
    }

    fun addRecurringBill(title: String, amount: Double, category: String, dueDay: Int) {
        viewModelScope.launch {
            recurringDao.insertRecurring(
                RecurringBill(
                    title = title,
                    amount = amount,
                    category = category,
                    dueDay = dueDay
                )
            )
        }
    }

    fun updateRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            recurringDao.updateRecurring(bill)
        }
    }

    fun deleteRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            recurringDao.deleteRecurring(bill)
        }
    }

    fun payRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            expenseDao.insertExpense(
                Expense(
                    title = "[Tagihan] ${bill.title}",
                    amount = bill.amount,
                    category = bill.category,
                    date = System.currentTimeMillis()
                )
            )
            recurringDao.updateRecurring(bill.copy(lastPaidMonthYear = currentMonthYear))
        }
    }

    fun undoPayRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            val targetTitle = "[Tagihan] ${bill.title}"
            val existingExpense = expenseDao.getLatestExpenseByTitle(targetTitle)
            if (existingExpense != null) {
                expenseDao.deleteExpense(existingExpense)
            }
            recurringDao.updateRecurring(bill.copy(lastPaidMonthYear = ""))
        }
    }
}
