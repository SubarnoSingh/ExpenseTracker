package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.SubscriptionDao
import com.expensetracker.app.data.local.entities.toDomain
import com.expensetracker.app.data.local.entities.toEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.repository.CategoryDeleteResult
import com.expensetracker.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val subscriptionDao: SubscriptionDao,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCategoriesByType(type: CategoryType): Flow<List<Category>> =
        categoryDao.observeByType(type).map { list -> list.map { it.toDomain() } }

    override suspend fun getCategory(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    override suspend fun addCategory(category: Category): Long =
        categoryDao.insert(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        categoryDao.update(category.toEntity())

    override suspend fun deleteCategory(category: Category): CategoryDeleteResult {
        val expenseCount = expenseDao.countByCategory(category.id)
        val subscriptionCount = subscriptionDao.countByCategory(category.id)
        return if (expenseCount > 0 || subscriptionCount > 0) {
            CategoryDeleteResult.InUse(expenseCount, subscriptionCount)
        } else {
            categoryDao.delete(category.toEntity())
            CategoryDeleteResult.Deleted
        }
    }

    override suspend fun deleteAll() = categoryDao.deleteAll()
}
