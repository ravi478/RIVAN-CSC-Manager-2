package com.rivan.cscmanager.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM shops ORDER BY id DESC") fun shops(): Flow<List<Shop>>
    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1") suspend fun shop(id: Long): Shop?
    @Insert suspend fun addShop(shop: Shop): Long
    @Query("UPDATE shops SET name=:name, code=:code, address=:address WHERE id=:id") suspend fun updateShop(id: Long, name: String, code: String, address: String)
    @Query("UPDATE shops SET active=:active WHERE id=:id") suspend fun setShopActive(id: Long, active: Boolean)

    @Query("SELECT * FROM operators ORDER BY id DESC") fun operators(): Flow<List<Operator>>
    @Query("SELECT * FROM operators WHERE loginId=:loginId AND password=:password AND active=1 LIMIT 1") suspend fun operatorLogin(loginId: String, password: String): Operator?
    @Insert suspend fun addOperator(operator: Operator): Long
    @Query("UPDATE operators SET active=:active WHERE id=:id") suspend fun setOperatorActive(id: Long, active: Boolean)

    @Query("SELECT * FROM services WHERE active=1 ORDER BY name") fun services(): Flow<List<ServiceItem>>
    @Insert suspend fun addService(service: ServiceItem): Long
    @Query("UPDATE services SET name=:name, category=:category, price=:price WHERE id=:id") suspend fun updateService(id: Long, name: String, category: String, price: Double)
    @Query("UPDATE services SET active=0 WHERE id=:id") suspend fun deactivateService(id: Long)

    @Query("SELECT * FROM customers ORDER BY id DESC") fun customers(): Flow<List<Customer>>
    @Query("SELECT * FROM customers WHERE shopId=:shopId ORDER BY id DESC") fun customersForShop(shopId: Long): Flow<List<Customer>>
    @Insert suspend fun addCustomer(customer: Customer): Long

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") fun transactions(): Flow<List<TransactionEntry>>
    @Query("SELECT * FROM transactions WHERE shopId=:shopId ORDER BY createdAt DESC") fun transactionsForShop(shopId: Long): Flow<List<TransactionEntry>>
    @Insert suspend fun addTransaction(entry: TransactionEntry): Long

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC") fun expenses(): Flow<List<Expense>>
    @Query("SELECT * FROM expenses WHERE shopId=:shopId ORDER BY createdAt DESC") fun expensesForShop(shopId: Long): Flow<List<Expense>>
    @Insert suspend fun addExpense(expense: Expense): Long

    @Query("SELECT * FROM attendance WHERE operatorId=:operatorId AND checkOut IS NULL ORDER BY id DESC LIMIT 1") suspend fun openAttendance(operatorId: Long): Attendance?
    @Insert suspend fun addAttendance(attendance: Attendance): Long
    @Query("UPDATE attendance SET checkOut=:time WHERE id=:id") suspend fun checkOut(id: Long, time: Long)
    @Query("SELECT * FROM attendance ORDER BY checkIn DESC") fun attendance(): Flow<List<Attendance>>

    @Insert suspend fun addAudit(log: AuditLog)
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 200") fun auditLogs(): Flow<List<AuditLog>>
    @Query("SELECT COUNT(*) FROM shops") fun shopCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM operators") fun operatorCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM customers") fun customerCount(): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions") fun totalIncome(): Flow<Double>
    @Query("SELECT COALESCE(SUM(amount),0) FROM expenses") fun totalExpenses(): Flow<Double>
}

@Database(entities = [Shop::class, Operator::class, ServiceItem::class, Customer::class, TransactionEntry::class, Expense::class, Attendance::class, AuditLog::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rivan_csc_manager.db").build().also { INSTANCE = it }
        }
    }
}
