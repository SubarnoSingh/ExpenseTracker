package com.expensetracker.app.presentation.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.DateGroup
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.SubscriptionEntry
import com.expensetracker.app.domain.model.TimePeriod
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.repository.SubscriptionRepository
import com.expensetracker.app.domain.usecase.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

enum class ExpensesSegment { REGULAR, OCCASIONAL, SUBSCRIPTIONS }

data class ExpensesUiState(
    val segment: ExpensesSegment = ExpensesSegment.REGULAR,
    val query: String = "",
    val filterCategoryId: Long? = null,
    val dateRange: ClosedRange<LocalDate>? = null,
    val categories: List<Category> = emptyList(),
    val dateGroups: List<DateGroup> = emptyList(),
    val total: Double = 0.0,
    val subscriptions: List<SubscriptionEntry> = emptyList(),
    val subscriptionMonthlyCost: Double = 0.0,
    val currency: Currency = Currency.INR,
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val segment = MutableStateFlow(ExpensesSegment.REGULAR)
    private val search = MutableStateFlow("")
    private val filterCategoryId = MutableStateFlow<Long?>(null)
    private val dateRange = MutableStateFlow<ClosedRange<LocalDate>?>(null)
    val filterActive: StateFlow<Boolean> = combine(filterCategoryId, dateRange) { cat, range ->
        cat != null || range != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun selectSegment(value: ExpensesSegment) {
        segment.value = value
        filterCategoryId.value = null
        dateRange.value = null
    }

    fun setQuery(value: String) { search.value = value }
    fun setCategoryFilter(id: Long?) { filterCategoryId.value = id }
    fun setDateRange(range: ClosedRange<LocalDate>?) { dateRange.value = range }
    fun resetFilters() {
        filterCategoryId.value = null
        dateRange.value = null
    }

    private data class AllData(
        val categories: List<Category>,
        val entries: List<ExpenseEntry>,
        val subscriptions: List<SubscriptionEntry>,
        val currency: Currency,
    )

    private data class Filter(
        val segment: ExpensesSegment,
        val query: String,
        val categoryId: Long?,
        val range: ClosedRange<LocalDate>?,
    )

    private val dataFlow = combine(
        categoryRepository.observeCategories(),
        expenseRepository.observeEntries(),
        subscriptionRepository.observeSubscriptions(),
        settingsRepository.settings,
    ) { categories, entries, subscriptions, settings ->
        AllData(
            categories = categories,
            entries = entries,
            subscriptions = subscriptions,
            currency = Currency.fromCode(settings.currency),
        )
    }

    private val filterFlow = combine(
        segment,
        search.debounce(250).distinctUntilChanged(),
        filterCategoryId,
        dateRange,
    ) { segment, query, categoryId, range ->
        Filter(segment, query.trim().lowercase(), categoryId, range)
    }

    val uiState: StateFlow<ExpensesUiState> = combine(dataFlow, filterFlow) { data, filter ->
        val isSubscriptionSegment = filter.segment == ExpensesSegment.SUBSCRIPTIONS

        if (isSubscriptionSegment) {
            val filteredSubs = data.subscriptions.filter { sub ->
                filter.query.isEmpty() ||
                    sub.subscription.name.lowercase().contains(filter.query) ||
                    (sub.category?.name?.lowercase()?.contains(filter.query) == true)
            }
            ExpensesUiState(
                segment = filter.segment,
                query = search.value,
                filterCategoryId = filter.categoryId,
                dateRange = filter.range,
                categories = data.categories.filter { it.type == com.expensetracker.app.domain.model.CategoryType.SUBSCRIPTION },
                subscriptions = filteredSubs,
                subscriptionMonthlyCost = StatsCalculator.subscriptionMonthlyCost(filteredSubs),
                currency = data.currency,
                loading = false,
            )
        } else {
            val type = when (filter.segment) {
                ExpensesSegment.REGULAR -> ExpenseType.REGULAR
                ExpensesSegment.OCCASIONAL -> ExpenseType.OCCASIONAL
                ExpensesSegment.SUBSCRIPTIONS -> error("handled above")
            }
            val filtered = data.entries.filter { entry ->
                val expense = entry.expense
                expense.type == type &&
                    (filter.categoryId == null || expense.categoryId == filter.categoryId) &&
                    (filter.range == null || expense.date in filter.range) &&
                    (filter.query.isEmpty() ||
                        expense.description.lowercase().contains(filter.query) ||
                        entry.category.name.lowercase().contains(filter.query))
            }
            ExpensesUiState(
                segment = filter.segment,
                query = search.value,
                filterCategoryId = filter.categoryId,
                dateRange = filter.range,
                categories = data.categories.filter { it.type == type.toCategoryType() },
                dateGroups = StatsCalculator.groupByDate(filtered),
                total = StatsCalculator.total(filtered),
                currency = data.currency,
                loading = false,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesUiState(),
    )
}

private fun ExpenseType.toCategoryType(): com.expensetracker.app.domain.model.CategoryType = when (this) {
    ExpenseType.REGULAR -> com.expensetracker.app.domain.model.CategoryType.REGULAR
    ExpenseType.OCCASIONAL -> com.expensetracker.app.domain.model.CategoryType.OCCASIONAL
}

/** Convenience helpers for the filter sheet. */
fun expensesPresetRange(preset: TimePeriod, now: LocalDate = LocalDate.now()): ClosedRange<LocalDate>? =
    preset.range(now)
