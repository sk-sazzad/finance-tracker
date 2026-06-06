package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.FinanceDao
import com.example.data.local.entities.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        WalletEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        SavingsDepositEntity::class,
        RecurringTransactionEntity::class,
        DebtPersonEntity::class,
        DebtEntryEntity::class,
        BillEntity::class,
        BadgeEntity::class,
        ExchangeRateEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao
    abstract fun debtDao(): com.example.data.local.dao.DebtDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tracker_db"
                )
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2,
                    DatabaseMigrations.MIGRATION_2_3,
                    DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5,
                    DatabaseMigrations.MIGRATION_5_6
                )
                // Database seed/callback section: 
                // Removed all default wallets, bills, and transactions. Keep categories and exchange rates.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
