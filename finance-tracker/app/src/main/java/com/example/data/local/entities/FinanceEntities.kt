package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", "TRANSFER"
    val categoryId: Long,
    val walletId: Long,
    val toWalletId: Long? = null, // Used for transfers between wallets
    val date: Long,
    val note: String,
    val tags: String, // Comma separated tags: "food,delivery"
    val currency: String,
    val isRecurring: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String, // Emoji or icon name
    val color: String, // Hex string
    val type: String, // "INCOME", "EXPENSE", "ALL"
    val isCustom: Boolean,
    val displayOrder: Int
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String, // Gradient HEX start or descriptor
    val balance: Double,
    val currency: String,
    val includeInTotal: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val month: Int, // 1 to 12
    val year: Int,
    val spent: Double = 0.0
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: Long,
    val linkedWalletId: Long? = null,
    val autoSave: Boolean = false,
    val description: String = ""
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE"
    val categoryId: Long,
    val walletId: Long,
    val note: String,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextDueDate: Long,
    val isActive: Boolean = true,
    val lastProcessed: Long = 0L
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Double,
    val type: String, // "LENT", "BORROWED"
    val date: Long,
    val dueDate: Long? = null,
    val paidAmount: Double = 0.0,
    val status: String, // "ACTIVE", "PARTIALLY_PAID", "SETTLED"
    val notes: String
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDate: Long,
    val categoryId: Long,
    val walletId: Long,
    val isRecurring: Boolean = true,
    val lastPaid: Long? = null
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val badgeKey: String, // e.g. "FIRST_TX", "SAVINGS_STARTER"
    val earnedDate: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String, // e.g. "USD", "EUR"
    val rate: Double, // Rate relative to 1 BDT (or relative to primary currency)
    val updatedAt: Long = System.currentTimeMillis()
)
