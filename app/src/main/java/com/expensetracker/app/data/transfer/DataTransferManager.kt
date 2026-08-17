package com.expensetracker.app.data.transfer

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.expensetracker.app.data.local.DefaultCategories
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entities.CategoryEntity
import com.expensetracker.app.data.local.entities.ExpenseEntity
import com.expensetracker.app.data.local.entities.SubscriptionEntity
import com.expensetracker.app.data.local.entities.toDomain
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionEntry
import com.expensetracker.app.domain.model.ThemeMode
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.usecase.CsvWriter
import com.expensetracker.app.domain.usecase.JsonBackup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun exportExpensesCsv(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            writeText(uri, CsvWriter.expensesToCsv(readAllExpenses()))
        }
    }

    suspend fun exportSubscriptionsCsv(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            writeText(uri, CsvWriter.subscriptionsToCsv(readAllSubscriptions()))
        }
    }

    suspend fun exportJsonBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsRepository.settings.first()
            val json = JsonBackup.toJson(
                userName = settings.userName,
                currency = settings.currency,
                themeMode = settings.themeMode.name,
                categories = readAllCategories(),
                expenses = readAllExpenses().map { it.expense },
                subscriptions = readAllSubscriptions().map { it.subscription },
            )
            writeText(uri, json)
        }
    }

    /**
     * Restores a JSON backup, replacing all current data. Category ids from the
     * backup are remapped to the freshly inserted rows so relationships survive.
     */
    suspend fun importJsonBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
                ?: error("Could not read backup file")
            val backup = JsonBackup.parse(text) ?: error("Backup file is invalid")

            database.withTransaction {
                database.expenseDao().deleteAll()
                database.subscriptionDao().deleteAll()
                database.categoryDao().deleteAll()

                // 1. Insert categories and build old-id -> new-id map.
                val idMap = mutableMapOf<Long, Long>()
                backup.categories.forEach { category ->
                    val newId = database.categoryDao().insert(category.toEntity())
                    idMap[category.id] = newId
                }

                // 2. Insert expenses with remapped category ids.
                backup.expenses.forEach { expense ->
                    val categoryId = resolveCategory(expense.categoryId, expense.type.toCategoryType())
                    database.expenseDao().insert(expense.toEntity(categoryId))
                }

                // 3. Insert subscriptions with remapped category ids.
                backup.subscriptions.forEach { subscription ->
                    val categoryId = resolveCategory(subscription.categoryId, CategoryType.SUBSCRIPTION)
                    database.subscriptionDao().insert(subscription.toEntity(categoryId))
                }
            }

            settingsRepository.setUserName(backup.userName)
            settingsRepository.setCurrency(backup.currency)
            settingsRepository.setThemeMode(
                runCatching { ThemeMode.valueOf(backup.themeMode) }
                    .getOrDefault(ThemeMode.DARK)
            )
        }
    }

    suspend fun deleteAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                database.expenseDao().deleteAll()
                database.subscriptionDao().deleteAll()
                database.categoryDao().deleteAll()
            }
            // Restore the default category set so the app stays usable.
            database.categoryDao().insertAll(DefaultCategories.all)
            Unit
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private suspend fun resolveCategory(oldId: Long, type: CategoryType): Long {
        val existing = database.categoryDao().observeByType(type).first()
            .firstOrNull { it.name == "Miscellaneous" }
        if (existing != null) return existing.id
        return database.categoryDao().insert(
            CategoryEntity(
                name = "Miscellaneous",
                icon = "more_horiz",
                color = 0xFF7C8599L,
                type = type,
            )
        )
    }

    private fun writeText(uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("Could not open destination file")
    }

    private suspend fun readAllCategories(): List<Category> =
        database.categoryDao().observeAll().first().map { it.toDomain() }

    private suspend fun readAllExpenses(): List<ExpenseEntry> =
        database.expenseDao().observeAllWithCategory().first().map { it.toDomain() }

    private suspend fun readAllSubscriptions(): List<SubscriptionEntry> =
        database.subscriptionDao().observeAllWithCategory().first().map { it.toDomain() }
}

// Small mapping helpers kept next to their call sites.
private fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id, name = name, icon = icon, color = color, type = type,
)

private fun Expense.toEntity(categoryId: Long): ExpenseEntity = ExpenseEntity(
    id = 0,
    amount = amount,
    description = description,
    categoryId = categoryId,
    type = type,
    date = date,
    time = time,
    note = note,
    createdAt = System.currentTimeMillis(),
)

private fun Subscription.toEntity(categoryId: Long): SubscriptionEntity = SubscriptionEntity(
    id = 0,
    name = name,
    amount = amount,
    categoryId = categoryId,
    billingCycle = billingCycle,
    startDate = startDate,
    nextBillingDate = nextBillingDate,
    note = note,
    isActive = isActive,
)

private fun ExpenseType.toCategoryType(): CategoryType = when (this) {
    ExpenseType.REGULAR -> CategoryType.REGULAR
    ExpenseType.OCCASIONAL -> CategoryType.OCCASIONAL
}
