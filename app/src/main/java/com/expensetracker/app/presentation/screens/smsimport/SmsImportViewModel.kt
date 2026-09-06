package com.expensetracker.app.presentation.screens.smsimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.sms.SmsExpense
import com.expensetracker.app.data.sms.SmsReader
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.toCategoryType
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One scanned message in the review list. */
data class SmsImportRow(
    val sms: SmsExpense,
    val selected: Boolean = true,
)

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val imported: Int) : ScanState
}

@HiltViewModel
class SmsImportViewModel @Inject constructor(
    private val smsReader: SmsReader,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val rows = MutableStateFlow<List<SmsImportRow>>(emptyList())
    val scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val type = MutableStateFlow(ExpenseType.REGULAR)
    val selectedCategoryId = MutableStateFlow<Long?>(null)

    val categories: StateFlow<List<Category>> = type
        .flatMapLatest { categoryRepository.observeCategoriesByType(it.toCategoryType()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<Currency> = settingsRepository.settings
        .map { Currency.fromCode(it.currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.INR)

    init {
        // Default to the first category of the selected type, same as the add sheet.
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

    fun setCategory(id: Long) { selectedCategoryId.value = id }

    fun toggle(smsId: Long) {
        rows.value = rows.value.map {
            if (it.sms.smsId == smsId) it.copy(selected = !it.selected) else it
        }
    }

    fun setAllSelected(selected: Boolean) {
        rows.value = rows.value.map { it.copy(selected = selected) }
    }

    /** Reads the inbox from the last-import watermark forward. */
    fun scan() {
        viewModelScope.launch {
            scanState.value = ScanState.Scanning
            val since = settingsRepository.lastSmsImportAt.first()
            rows.value = smsReader.readDebits(since).map { SmsImportRow(it) }
            scanState.value = ScanState.Idle
        }
    }

    fun importSelected() {
        val categoryId = selectedCategoryId.value ?: return
        val chosen = rows.value.filter { it.selected }
        if (chosen.isEmpty()) return

        viewModelScope.launch {
            chosen.forEach { row ->
                expenseRepository.addExpense(
                    Expense(
                        amount = row.sms.amount,
                        description = row.sms.merchant,
                        categoryId = categoryId,
                        type = type.value,
                        date = row.sms.date,
                        time = row.sms.time,
                        note = "Imported from SMS${row.sms.sender.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty()}",
                    )
                )
            }
            // Watermark past every message shown this scan, imported or not - a row
            // left unticked was a deliberate "no", so don't offer it again.
            rows.value.maxOfOrNull { it.sms.sentAt }
                ?.let { settingsRepository.setLastSmsImportAt(it) }
            rows.value = emptyList()
            scanState.value = ScanState.Done(chosen.size)
        }
    }

    fun clearResult() { scanState.value = ScanState.Idle }
}
