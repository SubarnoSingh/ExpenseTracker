package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.entities.toDomain
import com.expensetracker.app.data.local.entities.toEntity
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
) : ExpenseRepository {

    override fun observeEntries(): Flow<List<ExpenseEntry>> =
        dao.observeAllWithCategory().map { rows -> rows.map { it.toDomain() } }

    override fun observeEntriesByType(type: ExpenseType): Flow<List<ExpenseEntry>> =
        dao.observeWithCategoryByType(type).map { rows -> rows.map { it.toDomain() } }

    override fun observeEntriesByCategory(categoryId: Long): Flow<List<ExpenseEntry>> =
        dao.observeWithCategoryByCategory(categoryId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getExpense(id: Long): Expense? = dao.getById(id)?.toDomain()

    override suspend fun addExpense(expense: Expense): Long = dao.insert(expense.toEntity())

    override suspend fun updateExpense(expense: Expense) = dao.update(expense.toEntity())

    override suspend fun deleteExpense(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()
}
