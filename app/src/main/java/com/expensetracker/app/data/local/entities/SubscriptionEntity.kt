package com.expensetracker.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionEntry
import java.time.LocalDate

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("categoryId")],
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val categoryId: Long,
    val billingCycle: BillingCycle,
    val startDate: LocalDate,
    val nextBillingDate: LocalDate,
    val note: String? = null,
    val isActive: Boolean = true,
)

/** Row result joining a subscription with its category. */
data class SubscriptionWithCategory(
    @androidx.room.Embedded val subscription: SubscriptionEntity,
    @androidx.room.Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
)

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    name = name,
    amount = amount,
    categoryId = categoryId,
    billingCycle = billingCycle,
    startDate = startDate,
    nextBillingDate = nextBillingDate,
    note = note,
    isActive = isActive,
)

fun SubscriptionWithCategory.toDomain(): SubscriptionEntry = SubscriptionEntry(
    subscription = subscription.toDomain(),
    category = category?.toDomain(),
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    name = name,
    amount = amount,
    categoryId = categoryId,
    billingCycle = billingCycle,
    startDate = startDate,
    nextBillingDate = nextBillingDate,
    note = note,
    isActive = isActive,
)
