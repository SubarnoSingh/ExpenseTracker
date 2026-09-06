package com.expensetracker.app.presentation.screens.smsimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.sms.SmsCategoryGuess
import com.expensetracker.app.data.sms.SmsExpense
import com.expensetracker.app.data.sms.SmsReader
import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.toExpenseType
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** One scanned message in the review list, with the category it was sorted into. */
data class SmsImportRow(
    val sms: SmsExpense,
    val category: Category?,
    val selected: Boolean = true,
) {
    /** True when the guess landed on a real rule rather than the fallback. */
    val type: CategoryType? get() = category?.type
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val expenses: Int, val subscriptions: Int) : ScanState
}

@HiltViewModel
class SmsImportViewModel @Inject constructor(
    private val smsReader: SmsReader,
    private val expenseRepository: ExpenseRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val rows = MutableStateFlow<List<SmsImportRow>>(emptyList())
    val scanState = MutableStateFlow<ScanState>(ScanState.Idle)

    /** How far back the next scan reaches. Seeded from the last import in [prepare]. */
    val scanFrom = MutableStateFlow(LocalDate.now().minusDays(DEFAULT_SCAN_DAYS))

    /** Every category, so a row can be moved anywhere including subscriptions. */
    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<Currency> = settingsRepository.settings
        .map { Currency.fromCode(it.currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.INR)

    private var seeded = false

    /** Starts the scan window at the last import, so the common case is "what's new". */
    fun prepare() {
        if (seeded) return
        seeded = true
        viewModelScope.launch {
            val last = settingsRepository.lastSmsImportAt.first()
            if (last > 0) {
                scanFrom.value = java.time.Instant.ofEpochMilli(last)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            scan()
        }
    }

    fun setScanFrom(date: LocalDate) {
        scanFrom.value = date
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            scanState.value = ScanState.Scanning
            val since = scanFrom.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val all = categoryRepository.observeCategories().first()
            val fallback = all.firstOrNull { it.type == CategoryType.REGULAR }
            val byName = all.associateBy { it.name.lowercase() }
            val alreadyThere = existingKeys()

            rows.value = smsReader.readDebits(since)
                .filter { it.key() !in alreadyThere }
                .map { sms ->
                    val guess = SmsCategoryGuess.guess(sms.merchant, sms.body)
                    SmsImportRow(
                        sms = sms,
                        category = guess?.let { byName[it.lowercase()] } ?: fallback,
                    )
                }
            scanState.value = ScanState.Idle
        }
    }

    fun toggle(smsId: Long) {
        rows.value = rows.value.map {
            if (it.sms.smsId == smsId) it.copy(selected = !it.selected) else it
        }
    }

    fun setAllSelected(selected: Boolean) {
        rows.value = rows.value.map { it.copy(selected = selected) }
    }

    fun setRowCategory(smsId: Long, category: Category) {
        rows.value = rows.value.map {
            if (it.sms.smsId == smsId) it.copy(category = category) else it
        }
    }

    fun importSelected() {
        val chosen = rows.value.filter { it.selected && it.category != null }
        if (chosen.isEmpty()) return

        viewModelScope.launch {
            var expenses = 0
            var subscriptions = 0
            chosen.forEach { row ->
                val category = row.category ?: return@forEach
                val expenseType = category.type.toExpenseType()
                if (expenseType == null) {
                    subscriptionRepository.addSubscription(
                        Subscription(
                            name = row.sms.merchant,
                            amount = row.sms.amount,
                            categoryId = category.id,
                            // ponytail: monthly is the common case; edit the odd yearly one.
                            billingCycle = BillingCycle.MONTHLY,
                            startDate = row.sms.date,
                            nextBillingDate = row.sms.date.plusMonths(1),
                            note = row.sms.noteLabel(),
                        )
                    )
                    subscriptions++
                } else {
                    expenseRepository.addExpense(
                        Expense(
                            amount = row.sms.amount,
                            description = row.sms.merchant,
                            categoryId = category.id,
                            type = expenseType,
                            date = row.sms.date,
                            time = row.sms.time,
                            note = row.sms.noteLabel(),
                        )
                    )
                    expenses++
                }
            }
            // Watermark past every message shown this scan, imported or not - a row
            // left unticked was a deliberate "no", so don't offer it again.
            rows.value.maxOfOrNull { it.sms.sentAt }
                ?.let { settingsRepository.setLastSmsImportAt(it) }
            rows.value = emptyList()
            scanState.value = ScanState.Done(expenses, subscriptions)
        }
    }

    fun clearResult() { scanState.value = ScanState.Idle }

    /**
     * Keys of what's already saved, so widening the date range past a previous
     * import re-offers nothing.
     */
    private suspend fun existingKeys(): Set<String> {
        val expenses = expenseRepository.observeEntries().first()
            .map { "${it.expense.amount}|${it.expense.date}|${it.expense.description}" }
        val subs = subscriptionRepository.observeSubscriptions().first()
            .map { "${it.subscription.amount}|${it.subscription.startDate}|${it.subscription.name}" }
        return (expenses + subs).toSet()
    }

    private fun SmsExpense.key() = "$amount|$date|$merchant"

    private fun SmsExpense.noteLabel() =
        "Imported from SMS" + sender.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty()

    private companion object {
        const val DEFAULT_SCAN_DAYS = 90L
    }
}
