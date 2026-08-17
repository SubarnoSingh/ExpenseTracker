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
import com.expensetracker.app.`data`.local.entities.ExpenseEntity
import com.expensetracker.app.`data`.local.entities.ExpenseWithCategory
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.ExpenseType
import java.time.LocalDate
import java.time.LocalTime
import javax.`annotation`.processing.Generated
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
public class ExpenseDao_Impl(
  __db: RoomDatabase,
) : ExpenseDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfExpenseEntity: EntityInsertAdapter<ExpenseEntity>

  private val __converters: Converters = Converters()

  private val __deleteAdapterOfExpenseEntity: EntityDeleteOrUpdateAdapter<ExpenseEntity>

  private val __updateAdapterOfExpenseEntity: EntityDeleteOrUpdateAdapter<ExpenseEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfExpenseEntity = object : EntityInsertAdapter<ExpenseEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `expenses` (`id`,`amount`,`description`,`categoryId`,`type`,`date`,`time`,`note`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ExpenseEntity) {
        statement.bindLong(1, entity.id)
        statement.bindDouble(2, entity.amount)
        statement.bindText(3, entity.description)
        statement.bindLong(4, entity.categoryId)
        val _tmp: String = __converters.expenseTypeToString(entity.type)
        statement.bindText(5, _tmp)
        val _tmp_1: Long = __converters.localDateToEpochDay(entity.date)
        statement.bindLong(6, _tmp_1)
        val _tmp_2: Int = __converters.localTimeToMinutes(entity.time)
        statement.bindLong(7, _tmp_2.toLong())
        val _tmpNote: String? = entity.note
        if (_tmpNote == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpNote)
        }
        statement.bindLong(9, entity.createdAt)
      }
    }
    this.__deleteAdapterOfExpenseEntity = object : EntityDeleteOrUpdateAdapter<ExpenseEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `expenses` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ExpenseEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfExpenseEntity = object : EntityDeleteOrUpdateAdapter<ExpenseEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `expenses` SET `id` = ?,`amount` = ?,`description` = ?,`categoryId` = ?,`type` = ?,`date` = ?,`time` = ?,`note` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ExpenseEntity) {
        statement.bindLong(1, entity.id)
        statement.bindDouble(2, entity.amount)
        statement.bindText(3, entity.description)
        statement.bindLong(4, entity.categoryId)
        val _tmp: String = __converters.expenseTypeToString(entity.type)
        statement.bindText(5, _tmp)
        val _tmp_1: Long = __converters.localDateToEpochDay(entity.date)
        statement.bindLong(6, _tmp_1)
        val _tmp_2: Int = __converters.localTimeToMinutes(entity.time)
        statement.bindLong(7, _tmp_2.toLong())
        val _tmpNote: String? = entity.note
        if (_tmpNote == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpNote)
        }
        statement.bindLong(9, entity.createdAt)
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun insert(expense: ExpenseEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfExpenseEntity.insertAndReturnId(_connection, expense)
    _result
  }

  public override suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfExpenseEntity.insertAndReturnIdsList(_connection, expenses)
    _result
  }

  public override suspend fun delete(expense: ExpenseEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfExpenseEntity.handle(_connection, expense)
  }

  public override suspend fun update(expense: ExpenseEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfExpenseEntity.handle(_connection, expense)
  }

  public override fun observeWithCategoryByType(type: ExpenseType): Flow<List<ExpenseWithCategory>> {
    val _sql: String = "SELECT * FROM expenses WHERE type = ? ORDER BY date DESC, time DESC"
    return createFlow(__db, true, arrayOf("categories", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __converters.expenseTypeToString(type)
        _stmt.bindText(_argIndex, _tmp)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _collectionCategory: LongSparseArray<CategoryEntity?> = LongSparseArray<CategoryEntity?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfCategoryId)
          _collectionCategory.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection, _collectionCategory)
        val _result: MutableList<ExpenseWithCategory> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseWithCategory
          val _tmpExpense: ExpenseEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpType: ExpenseType
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.stringToExpenseType(_tmp_1)
          val _tmpDate: LocalDate
          val _tmp_2: Long
          _tmp_2 = _stmt.getLong(_columnIndexOfDate)
          _tmpDate = __converters.epochDayToLocalDate(_tmp_2)
          val _tmpTime: LocalTime
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfTime).toInt()
          _tmpTime = __converters.minutesToLocalTime(_tmp_3)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _tmpExpense = ExpenseEntity(_tmpId,_tmpAmount,_tmpDescription,_tmpCategoryId,_tmpType,_tmpDate,_tmpTime,_tmpNote,_tmpCreatedAt)
          val _tmpCategory: CategoryEntity?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfCategoryId)
          _tmpCategory = _collectionCategory.get(_tmpKey_1)
          if (_tmpCategory == null) {
            error("Relationship item 'category' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'categoryId' and entityColumn named 'id'.")
          }
          _item = ExpenseWithCategory(_tmpExpense,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAllWithCategory(): Flow<List<ExpenseWithCategory>> {
    val _sql: String = "SELECT * FROM expenses ORDER BY date DESC, time DESC"
    return createFlow(__db, true, arrayOf("categories", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _collectionCategory: LongSparseArray<CategoryEntity?> = LongSparseArray<CategoryEntity?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfCategoryId)
          _collectionCategory.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection, _collectionCategory)
        val _result: MutableList<ExpenseWithCategory> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseWithCategory
          val _tmpExpense: ExpenseEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpType: ExpenseType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.stringToExpenseType(_tmp)
          val _tmpDate: LocalDate
          val _tmp_1: Long
          _tmp_1 = _stmt.getLong(_columnIndexOfDate)
          _tmpDate = __converters.epochDayToLocalDate(_tmp_1)
          val _tmpTime: LocalTime
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfTime).toInt()
          _tmpTime = __converters.minutesToLocalTime(_tmp_2)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _tmpExpense = ExpenseEntity(_tmpId,_tmpAmount,_tmpDescription,_tmpCategoryId,_tmpType,_tmpDate,_tmpTime,_tmpNote,_tmpCreatedAt)
          val _tmpCategory: CategoryEntity?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfCategoryId)
          _tmpCategory = _collectionCategory.get(_tmpKey_1)
          if (_tmpCategory == null) {
            error("Relationship item 'category' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'categoryId' and entityColumn named 'id'.")
          }
          _item = ExpenseWithCategory(_tmpExpense,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeWithCategoryByCategory(categoryId: Long): Flow<List<ExpenseWithCategory>> {
    val _sql: String = "SELECT * FROM expenses WHERE categoryId = ? ORDER BY date DESC, time DESC"
    return createFlow(__db, true, arrayOf("categories", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, categoryId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _collectionCategory: LongSparseArray<CategoryEntity?> = LongSparseArray<CategoryEntity?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfCategoryId)
          _collectionCategory.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection, _collectionCategory)
        val _result: MutableList<ExpenseWithCategory> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseWithCategory
          val _tmpExpense: ExpenseEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpType: ExpenseType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.stringToExpenseType(_tmp)
          val _tmpDate: LocalDate
          val _tmp_1: Long
          _tmp_1 = _stmt.getLong(_columnIndexOfDate)
          _tmpDate = __converters.epochDayToLocalDate(_tmp_1)
          val _tmpTime: LocalTime
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfTime).toInt()
          _tmpTime = __converters.minutesToLocalTime(_tmp_2)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _tmpExpense = ExpenseEntity(_tmpId,_tmpAmount,_tmpDescription,_tmpCategoryId,_tmpType,_tmpDate,_tmpTime,_tmpNote,_tmpCreatedAt)
          val _tmpCategory: CategoryEntity?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfCategoryId)
          _tmpCategory = _collectionCategory.get(_tmpKey_1)
          if (_tmpCategory == null) {
            error("Relationship item 'category' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'categoryId' and entityColumn named 'id'.")
          }
          _item = ExpenseWithCategory(_tmpExpense,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeWithCategoryByCategoryAndType(categoryId: Long, type: ExpenseType): Flow<List<ExpenseWithCategory>> {
    val _sql: String = "SELECT * FROM expenses WHERE categoryId = ? AND type = ? ORDER BY date DESC, time DESC"
    return createFlow(__db, true, arrayOf("categories", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, categoryId)
        _argIndex = 2
        val _tmp: String = __converters.expenseTypeToString(type)
        _stmt.bindText(_argIndex, _tmp)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _collectionCategory: LongSparseArray<CategoryEntity?> = LongSparseArray<CategoryEntity?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfCategoryId)
          _collectionCategory.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection, _collectionCategory)
        val _result: MutableList<ExpenseWithCategory> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseWithCategory
          val _tmpExpense: ExpenseEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpType: ExpenseType
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.stringToExpenseType(_tmp_1)
          val _tmpDate: LocalDate
          val _tmp_2: Long
          _tmp_2 = _stmt.getLong(_columnIndexOfDate)
          _tmpDate = __converters.epochDayToLocalDate(_tmp_2)
          val _tmpTime: LocalTime
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfTime).toInt()
          _tmpTime = __converters.minutesToLocalTime(_tmp_3)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _tmpExpense = ExpenseEntity(_tmpId,_tmpAmount,_tmpDescription,_tmpCategoryId,_tmpType,_tmpDate,_tmpTime,_tmpNote,_tmpCreatedAt)
          val _tmpCategory: CategoryEntity?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfCategoryId)
          _tmpCategory = _collectionCategory.get(_tmpKey_1)
          if (_tmpCategory == null) {
            error("Relationship item 'category' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'categoryId' and entityColumn named 'id'.")
          }
          _item = ExpenseWithCategory(_tmpExpense,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeWithCategoryInRange(
    type: ExpenseType,
    startDate: LocalDate,
    endDate: LocalDate,
  ): Flow<List<ExpenseWithCategory>> {
    val _sql: String = "SELECT * FROM expenses WHERE type = ? AND date BETWEEN ? AND ? ORDER BY date DESC, time DESC"
    return createFlow(__db, true, arrayOf("categories", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __converters.expenseTypeToString(type)
        _stmt.bindText(_argIndex, _tmp)
        _argIndex = 2
        val _tmp_1: Long = __converters.localDateToEpochDay(startDate)
        _stmt.bindLong(_argIndex, _tmp_1)
        _argIndex = 3
        val _tmp_2: Long = __converters.localDateToEpochDay(endDate)
        _stmt.bindLong(_argIndex, _tmp_2)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _collectionCategory: LongSparseArray<CategoryEntity?> = LongSparseArray<CategoryEntity?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfCategoryId)
          _collectionCategory.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipcategoriesAscomExpensetrackerAppDataLocalEntitiesCategoryEntity(_connection, _collectionCategory)
        val _result: MutableList<ExpenseWithCategory> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseWithCategory
          val _tmpExpense: ExpenseEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpType: ExpenseType
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.stringToExpenseType(_tmp_3)
          val _tmpDate: LocalDate
          val _tmp_4: Long
          _tmp_4 = _stmt.getLong(_columnIndexOfDate)
          _tmpDate = __converters.epochDayToLocalDate(_tmp_4)
          val _tmpTime: LocalTime
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfTime).toInt()
          _tmpTime = __converters.minutesToLocalTime(_tmp_5)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _tmpExpense = ExpenseEntity(_tmpId,_tmpAmount,_tmpDescription,_tmpCategoryId,_tmpType,_tmpDate,_tmpTime,_tmpNote,_tmpCreatedAt)
          val _tmpCategory: CategoryEntity?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfCategoryId)
          _tmpCategory = _collectionCategory.get(_tmpKey_1)
          if (_tmpCategory == null) {
            error("Relationship item 'category' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'categoryId' and entityColumn named 'id'.")
          }
          _item = ExpenseWithCategory(_tmpExpense,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): ExpenseEntity? {
    val _sql: String = "SELECT * FROM expenses WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: ExpenseEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCategoryId: Long
          _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          val _tmpType: ExpenseType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.stringToExpenseType(_tmp)
          val _tmpDate: LocalDate
          val _tmp_1: Long
          _tmp_1 = _stmt.getLong(_columnIndexOfDate)
          _tmpDate = __converters.epochDayToLocalDate(_tmp_1)
          val _tmpTime: LocalTime
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfTime).toInt()
          _tmpTime = __converters.minutesToLocalTime(_tmp_2)
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = ExpenseEntity(_tmpId,_tmpAmount,_tmpDescription,_tmpCategoryId,_tmpType,_tmpDate,_tmpTime,_tmpNote,_tmpCreatedAt)
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
    val _sql: String = "SELECT COUNT(*) FROM expenses WHERE categoryId = ?"
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
    val _sql: String = "DELETE FROM expenses WHERE id = ?"
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
    val _sql: String = "DELETE FROM expenses"
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
          val _item_1: CategoryEntity
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
