package com.example.pengeluaran.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.pengeluaran.data.AppDatabase
import com.example.pengeluaran.data.Expense
import com.example.pengeluaran.data.RecurringBill
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "expense_database"
    )
        .fallbackToDestructiveMigration() // Aman jika ada perubahan skema database
        .build()

    private val expenseDao = db.expenseDao()
    private val recurringBillDao = db.recurringBillDao()

    // Flow Pengeluaran Harian
    val expenses: StateFlow<List<Expense>> = expenseDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow Tagihan Rutin
    val recurringBills: StateFlow<List<RecurringBill>> = recurringBillDao.getAllRecurringBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(title: String, amount: Double, category: String) {
        viewModelScope.launch {
            expenseDao.insertExpense(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
        }
    }

    // Fungsi Tagihan Rutin yang dipanggil MainActivity
    fun addRecurringBill(name: String, amount: Double, dueDay: Int, category: String) {
        viewModelScope.launch {
            recurringBillDao.insertBill(
                RecurringBill(
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    category = category
                )
            )
        }
    }

    fun toggleBillPaidStatus(bill: RecurringBill) {
        viewModelScope.launch {
            recurringBillDao.updateBill(bill.copy(isPaidThisMonth = !bill.isPaidThisMonth))
        }
    }

    fun deleteRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            recurringBillDao.deleteBill(bill)
        }
    }
}
