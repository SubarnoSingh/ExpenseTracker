package com.expensetracker.app.presentation.screens.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.TimePeriod
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.usecase.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class CategoryDetailUiState(
    val category: Category? = null,
    val entries: List<ExpenseEntry> = emptyList(),
    val todayTotal: Double = 0.0,
    val weekTotal: Double = 0.0,
    val monthTotal: Double = 0.0,
    val currency: Currency = Currency.INR,
    val loading: Boolean = true,
)

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: 0L

    val uiState: StateFlow<CategoryDetailUiState> = combine(
        categoryRepository.observeCategories(),
        expenseRepository.observeEntriesByCategory(categoryId),
        settingsRepository.settings,
    ) { categories, entries, settings ->
        val now = LocalDate.now()
        val category = categories.firstOrNull { it.id == categoryId }
        val entriesOfType = entries.filter { entry ->
            category == null || entry.expense.type == category.type.toExpenseType()
        }
        CategoryDetailUiState(
            category = category,
            entries = entries,
            todayTotal = StatsCalculator.total(
                entriesOfType.filter { it.expense.date == now }
            ),
            weekTotal = StatsCalculator.total(
                entriesOfType.filter { it.expense.date in TimePeriod.WEEK.range(now) }
            ),
            monthTotal = StatsCalculator.total(
                entriesOfType.filter { it.expense.date in TimePeriod.MONTH.range(now) }
            ),
            currency = Currency.fromCode(settings.currency),
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryDetailUiState(),
    )
}

private fun com.expensetracker.app.domain.model.CategoryType.toExpenseType(): ExpenseType? =
    when (this) {
        com.expensetracker.app.domain.model.CategoryType.REGULAR -> ExpenseType.REGULAR
        com.expensetracker.app.domain.model.CategoryType.OCCASIONAL -> ExpenseType.OCCASIONAL
        com.expensetracker.app.domain.model.CategoryType.SUBSCRIPTION -> null
    }
