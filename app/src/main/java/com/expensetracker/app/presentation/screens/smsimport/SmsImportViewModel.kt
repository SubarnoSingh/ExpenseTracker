package com.expensetracker.app.presentation.screens.smsimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.imports.DuplicateGuard
import com.expensetracker.app.data.imports.ImportCandidate
import com.expensetracker.app.data.imports.ImportSource
import com.expensetracker.app.data.imports.SavedTransaction
import com.expensetracker.app.data.imports.referencesInNote
import com.expensetracker.app.data.sms.SmsCategoryGuess
import com.expensetracker.app.data.sms.SmsReader
import com.expensetracker.app.data.sms.merchantKey
import com.expensetracker.app.data.statement.StatementParser
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * One line in the review list.
 *
 * A subscription row stands for every charge from that merchant in the range, not
 * one message - [charges] says how many were folded together. [duplicateOf] is set
 * when the payment looks already recorded, which leaves the row visible but off.
 */
data class SmsImportRow(
    val candidate: ImportCandidate,
    val category: Category?,
    val selected: Boolean = true,
    val duplicateOf: String? = null,
    val charges: Int = 1,
    val firstCharge: LocalDate = candidate.date,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
) {
    val type: CategoryType? get() = category?.type
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val expenses: Int, val subscriptions: Int) : ScanState
}

/** Which source the review list currently holds. */
data class SourceInfo(
    val source: ImportSource = ImportSource.SMS,
    /** File name for a statement, empty for SMS. */
    val label: String = "",
    /** Rows the source held but didn't offer, e.g. money coming in. */
    val credits: Int = 0,
    val unreadable: Int = 0,
)

@HiltViewModel
class SmsImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsReader: SmsReader,
    private val expenseRepository: ExpenseRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val rows = MutableStateFlow<List<SmsImportRow>>(emptyList())
    val scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val sourceInfo = MutableStateFlow(SourceInfo())
    val errorMessage = MutableStateFlow<String?>(null)

    /** True while a save is in flight, so the Import button can't be double-tapped. */
    val importing = MutableStateFlow(false)

    /** How far back an SMS scan reaches. Seeded from the last import in [prepare]. */
    val scanFrom = MutableStateFlow(LocalDate.now().minusDays(DEFAULT_SCAN_DAYS))

    /** Every category, so a row can be moved anywhere including subscriptions. */
    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<Currency> = settingsRepository.settings
        .map { Currency.fromCode(it.currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.INR)

    private var seeded = false

    /**
     * Starts the window at the last import, so the common case is "what's new".
     *
     * With no previous import the window opens wide instead: there is nothing to
     * duplicate yet, and a short default silently truncates history - a 90 day
     * window on an empty database quietly drops every older message the inbox
     * still holds, which reads as lost money rather than an unscanned range.
     */
    fun prepare() {
        if (seeded) return
        seeded = true
        viewModelScope.launch {
            val last = settingsRepository.lastSmsImportAt.first()
            scanFrom.value = if (last > 0) {
                Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()).toLocalDate()
            } else {
                LocalDate.now().minusDays(FIRST_RUN_SCAN_DAYS)
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
            errorMessage.value = null
            val since = scanFrom.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            sourceInfo.value = SourceInfo(source = ImportSource.SMS)
            present(smsReader.readDebits(since))
        }
    }

    /** Reads a CSV statement the user picked and offers what it holds. */
    fun loadStatement(uri: Uri, displayName: String) {
        viewModelScope.launch {
            scanState.value = ScanState.Scanning
            errorMessage.value = null
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                errorMessage.value = "Couldn't read that file."
                rows.value = emptyList()
                scanState.value = ScanState.Idle
                return@launch
            }

            val result = StatementParser.parse(text, displayName)
            sourceInfo.value = SourceInfo(
                source = ImportSource.STATEMENT,
                label = displayName,
                credits = result.credits,
                unreadable = result.skipped,
            )
            if (result.error != null) {
                errorMessage.value = result.error
                rows.value = emptyList()
                scanState.value = ScanState.Idle
                return@launch
            }
            present(result.candidates)
        }
    }

    /** Categorises, folds subscriptions and flags anything already recorded. */
    private suspend fun present(candidates: List<ImportCandidate>) {
        val all = categoryRepository.observeCategories().first()
        val fallback = all.firstOrNull { it.type == CategoryType.REGULAR }
        val byName = all.associateBy { it.name.lowercase() }

        val classified = candidates.map { candidate ->
            val guess = SmsCategoryGuess.guess(candidate.merchant, candidate.body)
            SmsImportRow(
                candidate = candidate,
                category = guess?.let { byName[it.lowercase()] } ?: fallback,
            )
        }

        val (subscriptions, expenses) = classified.partition {
            it.type == CategoryType.SUBSCRIPTION
        }
        val folded = collapseSubscriptions(subscriptions) + expenses

        val flags = DuplicateGuard.flag(folded.map { it.candidate }, savedTransactions())
        rows.value = folded
            .map { row ->
                val reason = flags[row.candidate.id]
                row.copy(duplicateOf = reason, selected = reason == null)
            }
            .sortedByDescending { it.candidate.date }
        scanState.value = ScanState.Idle
    }

    fun toggle(id: String) {
        rows.value = rows.value.map {
            if (it.candidate.id == id) it.copy(selected = !it.selected) else it
        }
    }

    fun setAllSelected(selected: Boolean) {
        // "Select all" shouldn't quietly re-tick things flagged as already recorded.
        rows.value = rows.value.map {
            it.copy(selected = selected && (it.duplicateOf == null || it.selected))
        }
    }

    fun setRowCategory(id: String, category: Category) {
        rows.value = rows.value.map {
            if (it.candidate.id == id) it.copy(category = category) else it
        }
    }

    /** Lets a row be moved between monthly and yearly before it is saved. */
    fun toggleCycle(id: String) {
        rows.value = rows.value.map {
            if (it.candidate.id != id) it else it.copy(
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
                val candidate = row.candidate
                val expenseType = category.type.toExpenseType()
                if (expenseType == null) {
                    subscriptionRepository.addSubscription(
                        Subscription(
                            name = candidate.merchant,
                            amount = candidate.amount,
                            categoryId = category.id,
                            billingCycle = row.billingCycle,
                            startDate = row.firstCharge,
                            nextBillingDate = when (row.billingCycle) {
                                BillingCycle.MONTHLY -> candidate.date.plusMonths(1)
                                BillingCycle.YEARLY -> candidate.date.plusYears(1)
                            },
                            note = candidate.noteLabel(),
                        )
                    )
                    subscriptions++
                } else {
                    expenseRepository.addExpense(
                        Expense(
                            amount = candidate.amount,
                            description = candidate.merchant,
                            categoryId = category.id,
                            type = expenseType,
                            date = candidate.date,
                            time = candidate.time,
                            note = candidate.noteLabel(),
                        )
                    )
                    expenses++
                }
            }
            // Only an SMS scan moves the watermark - a statement covers its own dates
            // and says nothing about which messages have been dealt with.
            if (sourceInfo.value.source == ImportSource.SMS) {
                rows.value.maxOfOrNull { it.candidate.sentAtMillis() }
                    ?.let { settingsRepository.setLastSmsImportAt(it) }
            }
            rows.value = emptyList()
            scanState.value = ScanState.Done(expenses, subscriptions)
            importing.value = false
        }
    }

    fun clearResult() { scanState.value = ScanState.Idle }

    fun clearError() { errorMessage.value = null }

    /**
     * A recurring charge appears once per cycle, so N charges from one merchant are
     * one subscription, not N. Folds them, infers the cycle from the gaps, and drops
     * merchants already subscribed.
     */
    private suspend fun collapseSubscriptions(rows: List<SmsImportRow>): List<SmsImportRow> {
        val subscribed = subscriptionRepository.observeSubscriptions().first()
            .map { merchantKey(it.subscription.name) }
            .toSet()

        return rows.groupBy { merchantKey(it.candidate.merchant) }
            .filterKeys { it !in subscribed }
            .map { (_, charges) ->
                val newest = charges.maxBy { it.candidate.date }
                val dates = charges.map { it.candidate.date }.sorted()
                newest.copy(
                    charges = charges.size,
                    firstCharge = dates.first(),
                    billingCycle = inferCycle(dates),
                )
            }
    }

    /**
     * Two charges about a year apart is a yearly plan; anything tighter is monthly.
     * A single charge tells us nothing, so it stays monthly and the row offers a
     * pill to correct it.
     */
    private fun inferCycle(dates: List<LocalDate>): BillingCycle {
        if (dates.size < 2) return BillingCycle.MONTHLY
        val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }
        val typicalGap = gaps.sorted()[gaps.size / 2]
        return if (typicalGap >= YEARLY_GAP_DAYS) BillingCycle.YEARLY else BillingCycle.MONTHLY
    }

    /** Everything already saved, so either source can be checked against the other. */
    private suspend fun savedTransactions(): List<SavedTransaction> {
        val expenses = expenseRepository.observeEntries().first().map {
            SavedTransaction(
                amount = it.expense.amount,
                date = it.expense.date,
                merchant = it.expense.description,
                origin = it.expense.note.originLabel(),
                time = it.expense.time,
                references = referencesInNote(it.expense.note),
            )
        }
        val subs = subscriptionRepository.observeSubscriptions().first().map {
            SavedTransaction(
                amount = it.subscription.amount,
                date = it.subscription.startDate,
                merchant = it.subscription.name,
                origin = it.subscription.note.originLabel(),
                references = referencesInNote(it.subscription.note),
            )
        }
        return expenses + subs
    }

    private fun String?.originLabel(): String = when {
        this == null -> "an earlier entry"
        contains("statement", ignoreCase = true) -> "a statement"
        contains("SMS", ignoreCase = true) -> "SMS"
        else -> "an earlier entry"
    }

    private fun ImportCandidate.sentAtMillis(): Long =
        date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        const val DEFAULT_SCAN_DAYS = 90L

        /** No previous import means no risk of duplicates, so reach back properly. */
        const val FIRST_RUN_SCAN_DAYS = 730L
        const val YEARLY_GAP_DAYS = 200L
    }
}
