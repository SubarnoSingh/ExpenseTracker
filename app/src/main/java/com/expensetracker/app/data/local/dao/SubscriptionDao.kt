package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.expensetracker.app.data.local.entities.SubscriptionEntity
import com.expensetracker.app.data.local.entities.SubscriptionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Transaction
    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC, name COLLATE NOCASE")
    fun observeAllWithCategory(): Flow<List<SubscriptionWithCategory>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionEntity?

    @Query("SELECT COUNT(*) FROM subscriptions WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Insert
    suspend fun insert(subscription: SubscriptionEntity): Long

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Delete
    suspend fun delete(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(subscriptions: List<SubscriptionEntity>): List<Long>
}
