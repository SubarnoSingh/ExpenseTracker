package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.Subscription
import java.time.LocalDate
import java.time.LocalTime

/** Builds CSV text for all expenses (regular + occasional, never mixed). */
object CsvWriter {

    private fun escape(field: String): String {
        val needsQuotes = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = field.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    fun expensesToCsv(entries: List<com.expensetracker.app.domain.model.ExpenseEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("Type,Date,Time,Amount,Description,Category,Note")
        for (entry in entries) {
            val expense = entry.expense
            sb.appendLine(
                listOf(
                    expense.type.name,
                    expense.date.toString(),
                    expense.time.toString(),
                    "%.2f".format(expense.amount),
                    escape(expense.description),
                    escape(entry.category.name),
                    escape(expense.note.orEmpty()),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun subscriptionsToCsv(entries: List<com.expensetracker.app.domain.model.SubscriptionEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("Name,Amount,Cycle,StartDate,NextBillingDate,Category,Active,Note")
        for (entry in entries) {
            val sub = entry.subscription
            sb.appendLine(
                listOf(
                    escape(sub.name),
                    "%.2f".format(sub.amount),
                    sub.billingCycle.name,
                    sub.startDate.toString(),
                    sub.nextBillingDate.toString(),
                    escape(entry.category?.name.orEmpty()),
                    if (sub.isActive) "YES" else "NO",
                    escape(sub.note.orEmpty()),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }
}

/** Full JSON backup of settings, categories, expenses and subscriptions. */
object JsonBackup {

    private const val VERSION = 1

    fun toJson(
        userName: String,
        currency: String,
        themeMode: String,
        categories: List<Category>,
        expenses: List<Expense>,
        subscriptions: List<Subscription>,
    ): String {
        val root = org.json.JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put(
            "settings",
            org.json.JSONObject().apply {
                put("userName", userName)
                put("currency", currency)
                put("themeMode", themeMode)
            }
        )

        root.put(
            "categories",
            org.json.JSONArray().apply {
                categories.forEach { c ->
                    put(
                        org.json.JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("icon", c.icon)
                            put("color", c.color)
                            put("type", c.type.name)
                        }
                    )
                }
            }
        )

        root.put(
            "expenses",
            org.json.JSONArray().apply {
                expenses.forEach { e ->
                    put(
                        org.json.JSONObject().apply {
                            put("id", e.id)
                            put("amount", e.amount)
                            put("description", e.description)
                            put("categoryId", e.categoryId)
                            put("type", e.type.name)
                            put("date", e.date.toString())
                            put("time", e.time.toString())
                            put("note", e.note ?: "")
                        }
                    )
                }
            }
        )

        root.put(
            "subscriptions",
            org.json.JSONArray().apply {
                subscriptions.forEach { s ->
                    put(
                        org.json.JSONObject().apply {
                            put("id", s.id)
                            put("name", s.name)
                            put("amount", s.amount)
                            put("categoryId", s.categoryId)
                            put("billingCycle", s.billingCycle.name)
                            put("startDate", s.startDate.toString())
                            put("nextBillingDate", s.nextBillingDate.toString())
                            put("note", s.note ?: "")
                            put("isActive", s.isActive)
                        }
                    )
                }
            }
        )

        return root.toString(2)
    }

    data class Backup(
        val userName: String,
        val currency: String,
        val themeMode: String,
        val categories: List<Category>,
        val expenses: List<Expense>,
        val subscriptions: List<Subscription>,
    )

    fun parse(json: String): Backup? = try {
        val root = org.json.JSONObject(json)
        val settings = root.optJSONObject("settings")
        val categories = root.optJSONArray("categories")?.let(::parseCategories).orEmpty()
        val expenses = root.optJSONArray("expenses")?.let(::parseExpenses).orEmpty()
        val subscriptions = root.optJSONArray("subscriptions")?.let(::parseSubscriptions).orEmpty()
        Backup(
            userName = settings?.optString("userName") ?: "There",
            currency = settings?.optString("currency") ?: "INR",
            themeMode = settings?.optString("themeMode") ?: "DARK",
            categories = categories,
            expenses = expenses,
            subscriptions = subscriptions,
        )
    } catch (_: Exception) {
        null
    }

    private fun parseCategories(array: org.json.JSONArray): List<Category> =
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            Category(
                id = o.optLong("id"),
                name = o.optString("name"),
                icon = o.optString("icon"),
                color = o.optLong("color"),
                type = runCatching { CategoryType.valueOf(o.optString("type")) }.getOrDefault(CategoryType.REGULAR),
            )
        }

    private fun parseExpenses(array: org.json.JSONArray): List<Expense> =
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            Expense(
                id = o.optLong("id"),
                amount = o.optDouble("amount"),
                description = o.optString("description"),
                categoryId = o.optLong("categoryId"),
                type = runCatching { ExpenseType.valueOf(o.optString("type")) }.getOrDefault(ExpenseType.REGULAR),
                date = runCatching { LocalDate.parse(o.optString("date")) }.getOrDefault(LocalDate.now()),
                time = runCatching { LocalTime.parse(o.optString("time")) }.getOrDefault(LocalTime.NOON),
                note = o.optString("note").takeIf { it.isNotBlank() },
            )
        }

    private fun parseSubscriptions(array: org.json.JSONArray): List<Subscription> =
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            Subscription(
                id = o.optLong("id"),
                name = o.optString("name"),
                amount = o.optDouble("amount"),
                categoryId = o.optLong("categoryId"),
                billingCycle = runCatching { BillingCycle.valueOf(o.optString("billingCycle")) }.getOrDefault(BillingCycle.MONTHLY),
                startDate = runCatching { LocalDate.parse(o.optString("startDate")) }.getOrDefault(LocalDate.now()),
                nextBillingDate = runCatching { LocalDate.parse(o.optString("nextBillingDate")) }.getOrDefault(LocalDate.now()),
                note = o.optString("note").takeIf { it.isNotBlank() },
                isActive = o.optBoolean("isActive", true),
            )
        }
}
