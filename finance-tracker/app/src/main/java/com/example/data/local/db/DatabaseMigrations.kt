package com.example.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    // Migration 1 → 2
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add recurring_transactions table if it didn't exist in v1
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `amount` REAL NOT NULL,
                    `type` TEXT NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `walletId` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `frequency` TEXT NOT NULL,
                    `nextDueDate` INTEGER NOT NULL,
                    `isActive` INTEGER NOT NULL DEFAULT 1,
                    `lastProcessed` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }
    }

    // Migration 2 → 3
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add loans, bills, badges, exchange_rates tables
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `loans` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `personName` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `type` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `dueDate` INTEGER,
                    `paidAmount` REAL NOT NULL DEFAULT 0.0,
                    `status` TEXT NOT NULL,
                    `notes` TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `bills` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `dueDate` INTEGER NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `walletId` INTEGER NOT NULL,
                    `isRecurring` INTEGER NOT NULL DEFAULT 1,
                    `lastPaid` INTEGER
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `badges` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `badgeKey` TEXT NOT NULL,
                    `earnedDate` INTEGER NOT NULL,
                    `isSeen` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `exchange_rates` (
                    `currencyCode` TEXT PRIMARY KEY NOT NULL,
                    `rate` REAL NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    // Migration 3 → 4: Add debt_persons and debt_entries tables
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `debt_persons` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `personName` TEXT NOT NULL,
                    `phoneNumber` TEXT NOT NULL DEFAULT '',
                    `type` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `debt_entries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `personId` INTEGER NOT NULL,
                    `amount` REAL NOT NULL,
                    `reason` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `paidAmount` REAL NOT NULL DEFAULT 0.0,
                    `status` TEXT NOT NULL DEFAULT 'UNPAID',
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    // Migration 4 → 5: Add savings_deposits table
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `savings_deposits` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `goalId` INTEGER NOT NULL,
                    `amount` REAL NOT NULL,
                    `walletId` INTEGER,
                    `note` TEXT NOT NULL DEFAULT '',
                    `date` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    // Migration 5 → 6: Database version bump support
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema updates needed between 5 and 6, empty body acts as a successful migration fallback
        }
    }

    // FUTURE MIGRATION TEMPLATE
    // Whenever you add a new feature that changes the database,
    // add a new migration here following this pattern:
    //
    // val MIGRATION_5_6 = object : Migration(5, 6) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // ALTER TABLE or CREATE TABLE statements here
    //         // Example - add a new column:
    //         // db.execSQL("ALTER TABLE transactions ADD COLUMN `receipt` TEXT DEFAULT '' NOT NULL")
    //         // Example - add a new table:
    //         // db.execSQL("CREATE TABLE IF NOT EXISTS `new_table` (...)")
    //     }
    // }
}
