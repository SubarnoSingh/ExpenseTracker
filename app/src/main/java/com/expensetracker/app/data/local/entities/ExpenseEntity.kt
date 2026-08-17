package com.expensetracker.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("categoryId"), Index("date"), Index("type")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val categoryId: Long,
    val type: ExpenseType,
    /** Stored as epoch day for simple range queries. */
    val date: LocalDate,
    /** Stored as LocalTime via converter. */
    val time: LocalTime,
    val note: String? = null,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = 0,
)

/** Row result joining an expense with its category. */
data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity,
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amount = amount,
    description = description,
    categoryId = categoryId,
    type = type,
    date = date,
    time = time,
    note = note,
)

fun ExpenseWithCategory.toDomain(): ExpenseEntry = ExpenseEntry(
    expense = expense.toDomain(),
    category = category.toDomain(),
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amount = amount,
    description = description,
    categoryId = categoryId,
    type = type,
    date = date,
    time = time,
    note = note,
    createdAt = System.currentTimeMillis(),
)
