package com.example.pengeluaran.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Expense::class, RecurringBill::class, CategoryBudget::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringDao(): RecurringDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
}
