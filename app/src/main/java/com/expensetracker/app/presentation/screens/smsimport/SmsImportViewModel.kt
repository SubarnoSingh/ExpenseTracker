package com.expensetracker.app.presentation.screens.smsimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.sms.SmsCategoryGuess
import com.expensetracker.app.data.sms.SmsExpense
import com.expensetracker.app.data.sms.SmsReader
import com.expensetracker.app.data.sms.merchantKey
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * One line in the review list.
 *
 * A subscription row stands for every charge from that merchant in the scanned
 * range, not one message - [charges] says how many were folded together.
 */
data class SmsImportRow(
    val sms: SmsExpense,
    val category: Category?,
    val selected: Boolean = true,
    val charges: Int = 1,
    val firstCharge: LocalDate = sms.date,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
) {
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

    /** True while a save is in flight, so the Import button can't be double-tapped. */
    val importing = MutableStateFlow(false)

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
                scanFrom.value = Instant.ofEpochMilli(last)
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

            val found = smsReader.readDebits(since).map { sms ->
                val guess = SmsCategoryGuess.guess(sms.merchant, sms.body)
                SmsImportRow(
                    sms = sms,
                    category = guess?.let { byName[it.lowercase()] } ?: fallback,
                )
            }

            val (subscriptions, expenses) = found.partition {
                it.type == CategoryType.SUBSCRIPTION
            }
            val alreadySaved = existingExpenseKeys()
            rows.value = collapseSubscriptions(subscriptions) +
                expenses.filterNot { it.sms.expenseKey() in alreadySaved }
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

    /** Lets a row be moved between monthly and yearly before it is saved. */
    fun toggleCycle(smsId: Long) {
        rows.value = rows.value.map {
            if (it.sms.smsId != smsId) it else it.copy(
                billingCycle = if (it.billingCycle == BillingCycle.MONTHLY) {
                    BillingCycle.YEARLY
                } else {
                    BillingCycle.MONTHLY
                },
            )
        }
    }

    fun importSelected() {
        // rows aren't cleared until the coroutine below finishes, so without this a
        // second tap read the same list again and imported everything twice.
        if (importing.value) return
        val chosen = rows.value.filter { it.selected && it.category != null }
        if (chosen.isEmpty()) return
        importing.value = true

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
                            billingCycle = row.billingCycle,
                            startDate = row.firstCharge,
                            nextBillingDate = when (row.billingCycle) {
                                BillingCycle.MONTHLY -> row.sms.date.plusMonths(1)
                                BillingCycle.YEARLY -> row.sms.date.plusYears(1)
                            },
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
            importing.value = false
        }
    }

    fun clearResult() { scanState.value = ScanState.Idle }

    /**
     * A recurring charge sends one message per cycle, so N messages from the same
     * merchant are one subscription, not N. Folds them into a single row, infers the
     * cycle from the spacing between charges, and drops merchants already subscribed.
     */
    private suspend fun collapseSubscriptions(rows: List<SmsImportRow>): List<SmsImportRow> {
        val subscribed = subscriptionRepository.observeSubscriptions().first()
            .map { merchantKey(it.subscription.name) }
            .toSet()

        return rows.groupBy { merchantKey(it.sms.merchant) }
            .filterKeys { it !in subscribed }
            .map { (_, charges) ->
                val newest = charges.maxBy { it.sms.sentAt }
                val dates = charges.map { it.sms.date }.sorted()
                newest.copy(
                    charges = charges.size,
                    firstCharge = dates.first(),
                    billingCycle = inferCycle(dates),
                )
            }
    }

    /**
     * Two charges ~a year apart is a yearly plan; anything tighter is monthly.
     * A single charge tells us nothing, so it stays monthly - the common case.
     */
    private fun inferCycle(dates: List<LocalDate>): BillingCycle {
        if (dates.size < 2) return BillingCycle.MONTHLY
        val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }
        val typicalGap = gaps.sorted()[gaps.size / 2]
        return if (typicalGap >= YEARLY_GAP_DAYS) BillingCycle.YEARLY else BillingCycle.MONTHLY
    }

    /** One-off spends already saved, so widening the date range re-offers nothing. */
    private suspend fun existingExpenseKeys(): Set<String> =
        expenseRepository.observeEntries().first()
            .map { "${it.expense.amount}|${it.expense.date}|${it.expense.description}" }
            .toSet()

    private fun SmsExpense.expenseKey() = "$amount|$date|$merchant"

    private fun SmsExpense.noteLabel() =
        "Imported from SMS" + sender.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty()

    private companion object {
        const val DEFAULT_SCAN_DAYS = 90L
        const val YEARLY_GAP_DAYS = 200L
    }
}
