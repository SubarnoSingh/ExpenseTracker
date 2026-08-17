package com.expensetracker.app.presentation.screens.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionEntry
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.repository.SubscriptionRepository
import com.expensetracker.app.domain.usecase.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionsUiState(
    val subscriptions: List<SubscriptionEntry> = emptyList(),
    val categories: List<Category> = emptyList(),
    val monthlyCost: Double = 0.0,
    val yearlyCost: Double = 0.0,
    val currency: Currency = Currency.INR,
    val loading: Boolean = true,
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        subscriptionRepository.observeSubscriptions(),
        categoryRepository.observeCategoriesByType(CategoryType.SUBSCRIPTION),
        settingsRepository.settings,
    ) { subscriptions, categories, settings ->
        SubscriptionsUiState(
            subscriptions = subscriptions,
            categories = categories,
            monthlyCost = StatsCalculator.subscriptionMonthlyCost(subscriptions),
            yearlyCost = StatsCalculator.subscriptionYearlyCost(subscriptions),
            currency = Currency.fromCode(settings.currency),
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionsUiState(),
    )

    fun toggleActive(entry: SubscriptionEntry) {
        viewModelScope.launch {
            subscriptionRepository.updateSubscription(
                entry.subscription.copy(isActive = !entry.subscription.isActive)
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            subscriptionRepository.deleteSubscription(id)
        }
    }
}
