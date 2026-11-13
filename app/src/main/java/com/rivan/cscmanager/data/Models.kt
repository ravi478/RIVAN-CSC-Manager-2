package com.rivan.cscmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object Roles {
    const val ADMIN = "ADMIN"
    const val OPERATOR = "OPERATOR"
}

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val address: String = "",
    val active: Boolean = true
)

@Entity(tableName = "operators")
data class Operator(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val loginId: String,
    val password: String,
    val shopId: Long,
    val active: Boolean = true
)

@Entity(tableName = "services")
data class ServiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "General",
    val price: Double,
    val active: Boolean = true
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val address: String = "",
    val shopId: Long
)

@Entity(tableName = "transactions")
data class TransactionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val serviceName: String,
    val amount: Double,
    val paymentMode: String,
    val createdAt: Long = System.currentTimeMillis(),
    val shopId: Long,
    val operatorId: Long = 0
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val shopId: Long
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operatorId: Long,
    val shopId: Long,
    val checkIn: Long,
    val checkOut: Long? = null
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actor: String,
    val action: String,
    val detail: String,
    val createdAt: Long = System.currentTimeMillis()
)
