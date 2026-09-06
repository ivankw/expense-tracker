package com.example.pengeluaran.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_bills ORDER BY dueDay ASC")
    fun getAllRecurring(): Flow<List<RecurringBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(bill: RecurringBill)

    @Update
    suspend fun updateRecurring(bill: RecurringBill)

    @Delete
    suspend fun deleteRecurring(bill: RecurringBill)
}
