package com.example.pengeluaran.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets")
    fun getAllBudgets(): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: CategoryBudget)

    @Delete
    suspend fun deleteBudget(budget: CategoryBudget)
}
