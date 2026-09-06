package com.example.pengeluaran.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_bills ORDER BY dueDay ASC")
    fun getAllRecurringBills(): Flow<List<RecurringBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: RecurringBill)

    @Update
    suspend fun updateBill(bill: RecurringBill)

    @Delete
    suspend fun deleteBill(bill: RecurringBill)
}
