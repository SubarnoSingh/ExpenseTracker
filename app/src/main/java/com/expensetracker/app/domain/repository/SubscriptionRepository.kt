package com.expensetracker.app.domain.repository

import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionEntry
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeSubscriptions(): Flow<List<SubscriptionEntry>>
    suspend fun getSubscription(id: Long): Subscription?
    suspend fun addSubscription(subscription: Subscription): Long
    suspend fun updateSubscription(subscription: Subscription)
    suspend fun deleteSubscription(id: Long)
    suspend fun deleteAll()
}
