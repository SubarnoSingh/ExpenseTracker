package com.expensetracker.app.data.local

import com.expensetracker.app.data.local.entities.CategoryEntity
import com.expensetracker.app.domain.model.CategoryType

/**
 * Default categories installed on first launch. Users can add, edit, delete
 * and recolor these freely - nothing about categories is hardcoded elsewhere.
 */
object DefaultCategories {

    private const val COLOR_PURPLE = 0xFF8B5CF6L
    private const val COLOR_VIOLET = 0xFFA78BFAL
    private const val COLOR_INDIGO = 0xFF6366F1L
    private const val COLOR_BLUE = 0xFF3B82F6L
    private const val COLOR_CYAN = 0xFF06B6D4L
    private const val COLOR_TEAL = 0xFF14B8A6L
    private const val COLOR_GREEN = 0xFF22C55EL
    private const val COLOR_AMBER = 0xFFF59E0BL
    private const val COLOR_ORANGE = 0xFFF97316L
    private const val COLOR_PINK = 0xFFEC4899L
    private const val COLOR_ROSE = 0xFFF43F5EL
    private const val COLOR_GRAY = 0xFF7C8599L

    val all: List<CategoryEntity> = listOf(
        // ---- Regular ----
        CategoryEntity(name = "Food & Drinks", icon = "restaurant", color = COLOR_PURPLE, type = CategoryType.REGULAR),
        CategoryEntity(name = "Groceries", icon = "cart", color = COLOR_GREEN, type = CategoryType.REGULAR),
        CategoryEntity(name = "Transport", icon = "directions_bus", color = COLOR_BLUE, type = CategoryType.REGULAR),
        CategoryEntity(name = "Fuel", icon = "local_gas_station", color = COLOR_ORANGE, type = CategoryType.REGULAR),
        CategoryEntity(name = "Entertainment", icon = "movie", color = COLOR_PINK, type = CategoryType.REGULAR),
        CategoryEntity(name = "Education", icon = "school", color = COLOR_INDIGO, type = CategoryType.REGULAR),
        CategoryEntity(name = "Health", icon = "favorite", color = COLOR_ROSE, type = CategoryType.REGULAR),
        CategoryEntity(name = "Shopping", icon = "shopping_bag", color = COLOR_TEAL, type = CategoryType.REGULAR),
        CategoryEntity(name = "Bills", icon = "receipt_long", color = COLOR_AMBER, type = CategoryType.REGULAR),
        CategoryEntity(name = "Miscellaneous", icon = "more_horiz", color = COLOR_GRAY, type = CategoryType.REGULAR),

        // ---- Occasional ----
        CategoryEntity(name = "Clothes", icon = "checkroom", color = COLOR_VIOLET, type = CategoryType.OCCASIONAL),
        CategoryEntity(name = "Electronics", icon = "headphones", color = COLOR_CYAN, type = CategoryType.OCCASIONAL),
        CategoryEntity(name = "Travel", icon = "flight", color = COLOR_BLUE, type = CategoryType.OCCASIONAL),
        CategoryEntity(name = "Gifts", icon = "card_giftcard", color = COLOR_PINK, type = CategoryType.OCCASIONAL),
        CategoryEntity(name = "Large Purchases", icon = "kitchen", color = COLOR_INDIGO, type = CategoryType.OCCASIONAL),
        CategoryEntity(name = "Other", icon = "more_horiz", color = COLOR_GRAY, type = CategoryType.OCCASIONAL),

        // ---- Subscription ----
        CategoryEntity(name = "Music & Video", icon = "music_note", color = COLOR_PURPLE, type = CategoryType.SUBSCRIPTION),
        CategoryEntity(name = "Software & Cloud", icon = "cloud", color = COLOR_CYAN, type = CategoryType.SUBSCRIPTION),
        CategoryEntity(name = "Memberships", icon = "fitness_center", color = COLOR_GREEN, type = CategoryType.SUBSCRIPTION),
        CategoryEntity(name = "Domains & Hosting", icon = "language", color = COLOR_BLUE, type = CategoryType.SUBSCRIPTION),
        CategoryEntity(name = "Other Services", icon = "more_horiz", color = COLOR_GRAY, type = CategoryType.SUBSCRIPTION),
    )
}
