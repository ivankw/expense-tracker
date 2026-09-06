package com.example.pengeluaran.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_bills")
data class RecurringBill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val category: String = "Electricity",
    val isPaidThisMonth: Boolean = false
)
