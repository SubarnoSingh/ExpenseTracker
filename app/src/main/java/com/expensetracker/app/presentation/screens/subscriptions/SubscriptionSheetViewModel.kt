package com.expensetracker.app.presentation.screens.subscriptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.repository.SubscriptionRepository
import com.expensetracker.app.presentation.util.formatAmountForInput
import com.expensetracker.app.presentation.util.formatAmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SubscriptionSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val name = MutableStateFlow("")
    val amount = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val billingCycle = MutableStateFlow(BillingCycle.MONTHLY)
    val startDate = MutableStateFlow(LocalDate.now())
    val nextBillingDate = MutableStateFlow(LocalDate.now().plusMonths(1))
    val note = MutableStateFlow("")
    val isActive = MutableStateFlow(true)
    val editingId = MutableStateFlow<Long?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    val categories: StateFlow<List<Category>> = categoryRepository
        .observeCategoriesByType(CategoryType.SUBSCRIPTION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<Currency> = settingsRepository.settings
        .map { Currency.fromCode(it.currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.INR)

    init {
        val idArg = savedStateHandle.get<String>("subscriptionId")
        if (!idArg.isNullOrBlank()) {
            idArg.toLongOrNull()?.let { load(it) }
        } else {
            // Auto-select the first subscription category.
            viewModelScope.launch {
                categories.collect { list ->
                    if (selectedCategoryId.value == null && list.isNotEmpty()) {
                        selectedCategoryId.value = list.first().id
                    }
                }
            }
        }
    }

    fun load(id: Long) {
        viewModelScope.launch {
            subscriptionRepository.getSubscription(id)?.let { sub ->
                editingId.value = sub.id
                name.value = sub.name
                amount.value = formatAmountForInput(sub.amount)
                selectedCategoryId.value = sub.categoryId
                billingCycle.value = sub.billingCycle
                startDate.value = sub.startDate
                nextBillingDate.value = sub.nextBillingDate
                note.value = sub.note.orEmpty()
                isActive.value = sub.isActive
            }
        }
    }

    fun setName(value: String) { name.value = value }
    fun setAmount(raw: String) { amount.value = formatAmountInput(raw) }
    fun setCategory(id: Long) { selectedCategoryId.value = id; errorMessage.value = null }
    fun setNote(value: String) { note.value = value }
    fun setActive(value: Boolean) { isActive.value = value }

    fun setCycle(cycle: BillingCycle) {
        billingCycle.value = cycle
        nextBillingDate.value = computedNextBilling(startDate.value, cycle)
    }

    fun setStartDate(value: LocalDate) {
        startDate.value = value
        nextBillingDate.value = computedNextBilling(value, billingCycle.value)
    }

    fun setNextBillingDate(value: LocalDate) { nextBillingDate.value = value }

    fun save(onDone: () -> Unit) {
        val value = amount.value.toDoubleOrNull()
        val categoryId = selectedCategoryId.value
        when {
            name.value.isBlank() -> errorMessage.value = "Enter a subscription name"
            value == null || value <= 0 -> errorMessage.value = "Enter a valid amount"
            categoryId == null -> errorMessage.value = "Pick a category"
            nextBillingDate.value.isBefore(startDate.value) ->
                errorMessage.value = "Next billing date can't be before the start date"
            else -> {
                errorMessage.value = null
                viewModelScope.launch {
                    val subscription = Subscription(
                        id = editingId.value ?: 0,
                        name = name.value.trim(),
                        amount = value,
                        categoryId = categoryId,
                        billingCycle = billingCycle.value,
                        startDate = startDate.value,
                        nextBillingDate = nextBillingDate.value,
                        note = note.value.trim().ifBlank { null },
                        isActive = isActive.value,
                    )
                    if (editingId.value == null) {
                        subscriptionRepository.addSubscription(subscription)
                    } else {
                        subscriptionRepository.updateSubscription(subscription)
                    }
                    onDone()
                }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId.value ?: return
        viewModelScope.launch {
            subscriptionRepository.deleteSubscription(id)
            onDone()
        }
    }

    private fun computedNextBilling(start: LocalDate, cycle: BillingCycle): LocalDate =
        when (cycle) {
            BillingCycle.MONTHLY -> start.plusMonths(1)
            BillingCycle.YEARLY -> start.plusYears(1)
        }
}
