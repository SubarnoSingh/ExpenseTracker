package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.SubscriptionDao
import com.expensetracker.app.data.local.entities.toDomain
import com.expensetracker.app.data.local.entities.toEntity
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionEntry
import com.expensetracker.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val dao: SubscriptionDao,
) : SubscriptionRepository {

    override fun observeSubscriptions(): Flow<List<SubscriptionEntry>> =
        dao.observeAllWithCategory().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getSubscription(id: Long): Subscription? =
        dao.getById(id)?.toDomain()

    override suspend fun addSubscription(subscription: Subscription): Long =
        dao.insert(subscription.toEntity())

    override suspend fun updateSubscription(subscription: Subscription) =
        dao.update(subscription.toEntity())

    override suspend fun deleteSubscription(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()
}
