package com.expensetracker.app.`data`.local.dao

import androidx.collection.LongSparseArray
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.expensetracker.app.`data`.local.database.Converters
import com.expensetracker.app.`data`.local.entities.CategoryEntity
import com.expensetracker.app.`data`.local.entities.SubscriptionEntity
import com.expensetracker.app.`data`.local.entities.SubscriptionWithCategory
import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.CategoryType
import java.time.LocalDate
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SubscriptionDao_Impl(
  __db: RoomDatabase,
) : SubscriptionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubscriptionEntity: EntityInsertAdapter<SubscriptionEntity>

  private val __converters: Converters = Converters()

  private val __deleteAdapterOfSubscriptionEntity: EntityDeleteOrUpdateAdapter<SubscriptionEntity>

  private val __updateAdapterOfSubscriptionEntity: EntityDeleteOrUpdateAdapter<SubscriptionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSubscriptionEntity = object : EntityInsertAdapter<SubscriptionEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `subscriptions` (`id`,`name`,`amount`,`categoryId`,`billingCycle`,`startDate`,`nextBillingDate`,`note`,`isActive`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubscriptionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindDouble(3, entity.amount)
        statement.bindLong(4, entity.categoryId)
        val _tmp: String = __converters.billingCycleToString(entity.billingCycle)
        statement.bindText(5, _tmp)
        val _tmp_1: Long = __converters.localDateToEpochDay(entity.startDate)
        statement.bindLong(6, _tmp_1)
        val _tmp_2: Long = __converters.localDateToEpochDay(entity.nextBillingDate)
        statement.bindLong(7, _tmp_2)
        val _tmpNote: String? = entity.note
        if (_tmpNote == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpNote)
        }
        val _tmp_3: Int = if (entity.isActive) 1 else 0
        statement.bindLong(9, _tmp_3.toLong())
      }
    }
    this.__deleteAdapterOfSubscriptionEntity = object : EntityDeleteOrUpdateAdapter<SubscriptionEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `subscriptions` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubscriptionEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfSubscriptionEntity = object : EntityDeleteOrUpdateAdapter<SubscriptionEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `subscriptions` SET `id` = ?,`name` = ?,`amount` = ?,`categoryId` = ?,`billingCycle` = ?,`startDate` = ?,`nextBillingDate` = ?,`note` = ?,`isActive` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubscriptionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindDouble(3, entity.amount)
        statement.bindLong(4, entity.categoryId)
        val _tmp: String = __converters.billingCycleToString(entity.billingCycle)
        statement.bindText(5, _tmp)
        val _tmp_1: Long = __converters.localDateToEpochDay(entity.startDate)
        statement.bindLong(6, _tmp_1)
        val _tmp_2: Long = __converters.localDateToEpochDay(entity.nextBillingDate)
        statement.bindLong(7, _tmp_2)
        val _tmpNote: String? = entity.note
        if (_tmpNote == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpNote)
        }
        val _tmp_3: Int = if (entity.isActive) 1 else 0
        statement.bindLong(9, _tmp_3.toLong())
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun insert(subscription: SubscriptionEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfSubscriptionEntity.insertAndReturnId(_connection, subscription)
    _result
  }

  public override suspend fun insertAll(subscriptions: List<SubscriptionEntity>): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfSubscriptionEntity.insertAndReturnIdsList(_connection, subscriptions)
    _result
  }

  public override suspend fun delete(subscription: SubscriptionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfSubscriptionEntity.handle(_connection, subscription)
  }

  public override suspend fun update(subscription: SubscriptionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfSubscriptionEntity.handle(_connection, subscription)
  }

  public override fun observeAllWithCategory(): Flow<List<SubscriptionWithCategory>> {
    val _sql: String = "SELECT * FROM subscriptions ORDER BY nextBillingDate ASC, name COLLATE NOCASE"
    return createFlow(__db, true, arrayOf("categories", "subscriptions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfBillingCycle: Int = getColumnIndexOrThrow(_stmt, "billingCycle")
        val _columnIndexOfStartDate: Int = getColumnIndexOrThrow(_stmt, "startDate")
        val _columnIndexOfNextBillingDate: Int = getColumnIndexOrThrow(_stmt, "nextBillingDate")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _collectionCategory: LongSparseArray<CategoryEntity?> = LongSparseArray<CategoryEntity?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfCategoryId)
          _collectionCategory.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection, _collectionCategory)
        val _result: MutableList<SubscriptionWithCategory> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubscriptionWithCategory
          val _tmpSubscription: SubscriptionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpBillingCycle: BillingCycle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfBillingCycle)
          _tmpBillingCycle = __converters.stringToBillingCycle(_tmp)
          val _tmpStartDate: LocalDate
          val _tmp_1: Long
          _tmp_1 = _stmt.getLong(_columnIndexOfStartDate)
          _tmpStartDate = __converters.epochDayToLocalDate(_tmp_1)
          val _tmpNextBillingDate: LocalDate
          val _tmp_2: Long
          _tmp_2 = _stmt.getLong(_columnIndexOfNextBillingDate)
          _tmpNextBillingDate = __converters.epochDayToLocalDate(_tmp_2)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpIsActive: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_3 != 0
          _tmpSubscription = SubscriptionEntity(_tmpId,_tmpName,_tmpAmount,_tmpCategoryId,_tmpBillingCycle,_tmpStartDate,_tmpNextBillingDate,_tmpNote,_tmpIsActive)
          val _tmpCategory: CategoryEntity?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfCategoryId)
          _tmpCategory = _collectionCategory.get(_tmpKey_1)
          _item = SubscriptionWithCategory(_tmpSubscription,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): SubscriptionEntity? {
    val _sql: String = "SELECT * FROM subscriptions WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfBillingCycle: Int = getColumnIndexOrThrow(_stmt, "billingCycle")
        val _columnIndexOfStartDate: Int = getColumnIndexOrThrow(_stmt, "startDate")
        val _columnIndexOfNextBillingDate: Int = getColumnIndexOrThrow(_stmt, "nextBillingDate")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _result: SubscriptionEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpBillingCycle: BillingCycle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfBillingCycle)
          _tmpBillingCycle = __converters.stringToBillingCycle(_tmp)
          val _tmpStartDate: LocalDate
          val _tmp_1: Long
          _tmp_1 = _stmt.getLong(_columnIndexOfStartDate)
          _tmpStartDate = __converters.epochDayToLocalDate(_tmp_1)
          val _tmpNextBillingDate: LocalDate
          val _tmp_2: Long
          _tmp_2 = _stmt.getLong(_columnIndexOfNextBillingDate)
          _tmpNextBillingDate = __converters.epochDayToLocalDate(_tmp_2)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpIsActive: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_3 != 0
          _result = SubscriptionEntity(_tmpId,_tmpName,_tmpAmount,_tmpCategoryId,_tmpBillingCycle,_tmpStartDate,_tmpNextBillingDate,_tmpNote,_tmpIsActive)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countByCategory(categoryId: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM subscriptions WHERE categoryId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, categoryId)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM subscriptions WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM subscriptions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection: SQLiteConnection, _map: LongSparseArray<CategoryEntity?>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, false) { _tmpMap ->
        __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`name`,`icon`,`color`,`type` FROM `categories` WHERE `id` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "id")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfName: Int = 1
      val _columnIndexOfIcon: Int = 2
      val _columnIndexOfColor: Int = 3
      val _columnIndexOfType: Int = 4
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: CategoryEntity?
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIcon: String
          _tmpIcon = _stmt.getText(_columnIndexOfIcon)
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpType: CategoryType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.stringToCategoryType(_tmp)
          _item_1 = CategoryEntity(_tmpId,_tmpName,_tmpIcon,_tmpColor,_tmpType)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
