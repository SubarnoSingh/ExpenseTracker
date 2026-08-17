package com.expensetracker.app.data.local.database

import androidx.room.TypeConverter
import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.ExpenseType
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun localTimeToMinutes(time: LocalTime): Int = time.toSecondOfDay()

    @TypeConverter
    fun minutesToLocalTime(minutes: Int): LocalTime = LocalTime.ofSecondOfDay(minutes.toLong())

    @TypeConverter
    fun expenseTypeToString(type: ExpenseType): String = type.name

    @TypeConverter
    fun stringToExpenseType(value: String): ExpenseType = ExpenseType.valueOf(value)

    @TypeConverter
    fun categoryTypeToString(type: CategoryType): String = type.name

    @TypeConverter
    fun stringToCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun billingCycleToString(cycle: BillingCycle): String = cycle.name

    @TypeConverter
    fun stringToBillingCycle(value: String): BillingCycle = BillingCycle.valueOf(value)
}
