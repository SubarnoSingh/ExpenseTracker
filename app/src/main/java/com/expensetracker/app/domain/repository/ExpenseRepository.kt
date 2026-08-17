package com.expensetracker.app.domain.repository

import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeEntries(): Flow<List<ExpenseEntry>>
    fun observeEntriesByType(type: ExpenseType): Flow<List<ExpenseEntry>>
    fun observeEntriesByCategory(categoryId: Long): Flow<List<ExpenseEntry>>
    suspend fun getExpense(id: Long): Expense?
    suspend fun addExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(id: Long)
    suspend fun deleteAll()
}
