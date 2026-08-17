package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.expensetracker.app.data.local.entities.ExpenseEntity
import com.expensetracker.app.data.local.entities.ExpenseWithCategory
import com.expensetracker.app.domain.model.ExpenseType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExpenseDao {

    @Transaction
    @Query("SELECT * FROM expenses WHERE type = :type ORDER BY date DESC, time DESC")
    fun observeWithCategoryByType(type: ExpenseType): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC")
    fun observeAllWithCategory(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC, time DESC")
    fun observeWithCategoryByCategory(categoryId: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId AND type = :type ORDER BY date DESC, time DESC")
    fun observeWithCategoryByCategoryAndType(categoryId: Long, type: ExpenseType): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query(
        "SELECT * FROM expenses " +
            "WHERE type = :type AND date BETWEEN :startDate AND :endDate " +
            "ORDER BY date DESC, time DESC"
    )
    fun observeWithCategoryInRange(
        type: ExpenseType,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<ExpenseWithCategory>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long>
}
