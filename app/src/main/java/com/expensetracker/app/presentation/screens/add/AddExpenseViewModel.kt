package com.expensetracker.app.presentation.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.toCategoryType
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.presentation.util.formatAmountForInput
import com.expensetracker.app.presentation.util.formatAmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val amount = MutableStateFlow("")
    val description = MutableStateFlow("")
    val note = MutableStateFlow("")
    val date = MutableStateFlow(LocalDate.now())
    val time = MutableStateFlow(LocalTime.now().withSecond(0).withNano(0))
    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val type = MutableStateFlow(ExpenseType.REGULAR)
    val editingId = MutableStateFlow<Long?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    /** Categories matching the currently selected expense type. */
    val categories: StateFlow<List<Category>> = type
        .flatMapLatest { categoryRepository.observeCategoriesByType(it.toCategoryType()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<Currency> = settingsRepository.settings
        .map { Currency.fromCode(it.currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.INR)

    init {
        val typeArg = savedStateHandle.get<String>("type")
        val expenseIdArg = savedStateHandle.get<String>("expenseId")

        if (!expenseIdArg.isNullOrBlank()) {
            expenseIdArg.toLongOrNull()?.let { loadExpense(it) }
        } else if (!typeArg.isNullOrBlank()) {
            type.value = if (typeArg == "occasional") ExpenseType.OCCASIONAL else ExpenseType.REGULAR
        }

        // Auto-select the first category of the current type.
        viewModelScope.launch {
            categories.collect { list ->
                if (selectedCategoryId.value == null && list.isNotEmpty()) {
                    selectedCategoryId.value = list.first().id
                }
            }
        }
    }

    fun setType(value: ExpenseType) {
        type.value = value
        selectedCategoryId.value = null
    }

    fun setAmount(raw: String) {
        amount.value = formatAmountInput(raw)
    }

    fun setDescription(value: String) { description.value = value }
    fun setNote(value: String) { note.value = value }
    fun setCategory(id: Long) { selectedCategoryId.value = id; errorMessage.value = null }
    fun setDate(value: LocalDate) { date.value = value }
    fun setTime(value: LocalTime) { time.value = value }

    fun loadExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.getExpense(id)?.let { expense ->
                editingId.value = expense.id
                type.value = expense.type
                amount.value = formatAmountForInput(expense.amount)
                description.value = expense.description
                note.value = expense.note.orEmpty()
                date.value = expense.date
                time.value = expense.time
                selectedCategoryId.value = expense.categoryId
            }
        }
    }

    fun save(onDone: () -> Unit) {
        val value = amount.value.toDoubleOrNull()
        val categoryId = selectedCategoryId.value
        when {
            value == null || value <= 0 -> errorMessage.value = "Enter a valid amount"
            categoryId == null -> errorMessage.value = "Pick a category"
            else -> {
                errorMessage.value = null
                viewModelScope.launch {
                    val categoryName = categories.value.firstOrNull { it.id == categoryId }?.name
                    val expense = Expense(
                        id = editingId.value ?: 0,
                        amount = value,
                        description = description.value.trim().ifBlank { categoryName ?: "Expense" },
                        categoryId = categoryId,
                        type = type.value,
                        date = date.value,
                        time = time.value,
                        note = note.value.trim().ifBlank { null },
                    )
                    if (editingId.value == null) {
                        expenseRepository.addExpense(expense)
                    } else {
                        expenseRepository.updateExpense(expense)
                    }
                    onDone()
                }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId.value ?: return
        viewModelScope.launch {
            expenseRepository.deleteExpense(id)
            onDone()
        }
    }
}
