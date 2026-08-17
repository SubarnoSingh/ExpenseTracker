package com.expensetracker.app.domain.repository

import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

sealed interface CategoryDeleteResult {
    data object Deleted : CategoryDeleteResult
    data class InUse(val expenseCount: Int, val subscriptionCount: Int) : CategoryDeleteResult
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getCategory(id: Long): Category?
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category): CategoryDeleteResult
    suspend fun deleteAll()
}
