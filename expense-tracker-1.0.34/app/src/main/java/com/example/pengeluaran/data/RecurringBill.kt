package com.example.pengeluaran.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_bills")
data class RecurringBill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val dueDay: Int, // Tanggal jatuh tempo (1 - 31)
    val lastPaidMonthYear: String = "" // Format "yyyy-MM", menandai apakah sudah lunas bulan ini
)
