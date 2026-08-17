package com.expensetracker.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Key into the fixed icon set defined in presentation/components/CategoryIcons.kt */
    val icon: String,
    /** ARGB color value. */
    val color: Long,
    val type: CategoryType,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type,
)
