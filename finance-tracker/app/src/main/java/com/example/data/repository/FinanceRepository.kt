package com.example.data.repository

import com.example.data.local.dao.FinanceDao
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Calendar

class FinanceRepository(private val financeDao: FinanceDao) {

    // Streams of Data
    val allTransactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = financeDao.getAllCategories()
    val allWallets: Flow<List<WalletEntity>> = financeDao.getAllWallets()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = financeDao.getAllSavingsGoals()
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = financeDao.getAllRecurringTransactions()
    val allLoans: Flow<List<LoanEntity>> = financeDao.getAllLoans()
    val allBills: Flow<List<BillEntity>> = financeDao.getAllBills()
    val allBadges: Flow<List<BadgeEntity>> = financeDao.getAllBadges()
    val allExchangeRates: Flow<List<ExchangeRateEntity>> = financeDao.getAllExchangeRates()

    // --- POPULATION HELPERS ---
    suspend fun populateDefaultsIfEmpty() {
        // Populate Categories
        val existingCats = financeDao.getAllCategories().first()
        if (existingCats.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Food & Dining", icon = "🍔", color = "#FF6B6B", type = "EXPENSE", isCustom = false, displayOrder = 1),
                CategoryEntity(name = "Transport", icon = "🚗", color = "#4ECDC4", type = "EXPENSE", isCustom = false, displayOrder = 2),
                CategoryEntity(name = "Shopping", icon = "🛍️", color = "#A855F7", type = "EXPENSE", isCustom = false, displayOrder = 3),
                CategoryEntity(name = "Bills & Utilities", icon = "💡", color = "#F59E0B", type = "EXPENSE", isCustom = false, displayOrder = 4),
                CategoryEntity(name = "Entertainment", icon = "🎬", color = "#EC4899", type = "EXPENSE", isCustom = false, displayOrder = 5),
                CategoryEntity(name = "Health", icon = "🏥", color = "#10B981", type = "EXPENSE", isCustom = false, displayOrder = 6),
                CategoryEntity(name = "Education", icon = "📚", color = "#3B82F6", type = "EXPENSE", isCustom = false, displayOrder = 7),
                CategoryEntity(name = "Housing", icon = "🏠", color = "#8B5CF6", type = "EXPENSE", isCustom = false, displayOrder = 8),
                CategoryEntity(name = "Travel", icon = "✈️", color = "#06B6D4", type = "EXPENSE", isCustom = false, displayOrder = 9),
                CategoryEntity(name = "Investment", icon = "💰", color = "#84CC16", type = "EXPENSE", isCustom = false, displayOrder = 10),
                CategoryEntity(name = "Gifts", icon = "🎁", color = "#F97316", type = "EXPENSE", isCustom = false, displayOrder = 11),
                CategoryEntity(name = "Food Delivery", icon = "🍕", color = "#EF4444", type = "EXPENSE", isCustom = false, displayOrder = 12),
                CategoryEntity(name = "Salary", icon = "💵", color = "#00E676", type = "INCOME", isCustom = false, displayOrder = 13),
                CategoryEntity(name = "Bonus", icon = "⚡", color = "#00D4AA", type = "INCOME", isCustom = false, displayOrder = 14)
            )
            financeDao.insertCategories(defaultCategories)
        }

        // Populate Wallets
        // Start completely empty as requested

        // Populate Exchange Rates
        val rates = financeDao.getExchangeRatesSnapshot()
        if (rates.isEmpty()) {
            val defaultRates = listOf(
                ExchangeRateEntity("BDT", 1.0),
                ExchangeRateEntity("USD", 115.0),
                ExchangeRateEntity("EUR", 125.0),
                ExchangeRateEntity("GBP", 145.0),
                ExchangeRateEntity("SAR", 30.6),
                ExchangeRateEntity("AED", 31.3),
                ExchangeRateEntity("INR", 1.38),
                ExchangeRateEntity("SGD", 85.5)
            )
            financeDao.insertExchangeRates(defaultRates)
        }

        // Populate Default Bills if empty to show sweet UI
        // Start completely empty as requested

        // Populate Default Savings Goals
        // Start completely empty as requested


    }

    // --- TRANSACTION HANDLING ---
    suspend fun addTransaction(tx: TransactionEntity) {
        val txId = financeDao.insertTransaction(tx)
        // Adjust wallet balances accordingly
        val wallet = financeDao.getWalletById(tx.walletId)
        if (wallet != null) {
            when (tx.type) {
                "EXPENSE" -> {
                    val updatedWallet = wallet.copy(balance = wallet.balance - tx.amount)
                    financeDao.updateWallet(updatedWallet)
                }
                "INCOME" -> {
                    val updatedWallet = wallet.copy(balance = wallet.balance + tx.amount)
                    financeDao.updateWallet(updatedWallet)
                }
                "TRANSFER" -> {
                    // deduct from sender wallet
                    val updatedSender = wallet.copy(balance = wallet.balance - tx.amount)
                    financeDao.updateWallet(updatedSender)

                    // add to recipient wallet if toWalletId exists
                    if (tx.toWalletId != null) {
                        val receiver = financeDao.getWalletById(tx.toWalletId)
                        if (receiver != null) {
                            val updatedReceiver = receiver.copy(balance = receiver.balance + tx.amount)
                            financeDao.updateWallet(updatedReceiver)
                        }
                    }
                }
            }
        }

        // Trigger Achievement Unlocks
        val txCount = financeDao.getAllTransactions().first().size
        if (txCount >= 1) {
            unlockBadge("FIRST_TX")
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        financeDao.deleteTransaction(tx)
        // Reverse wallet balance impact
        val wallet = financeDao.getWalletById(tx.walletId)
        if (wallet != null) {
            when (tx.type) {
                "EXPENSE" -> {
                    val updatedWallet = wallet.copy(balance = wallet.balance + tx.amount)
                    financeDao.updateWallet(updatedWallet)
                }
                "INCOME" -> {
                    val updatedWallet = wallet.copy(balance = wallet.balance - tx.amount)
                    financeDao.updateWallet(updatedWallet)
                }
                "TRANSFER" -> {
                    // refund sender wallet
                    val updatedSender = wallet.copy(balance = wallet.balance + tx.amount)
                    financeDao.updateWallet(updatedSender)

                    // take back from receiver
                    if (tx.toWalletId != null) {
                        val receiver = financeDao.getWalletById(tx.toWalletId)
                        if (receiver != null) {
                            val updatedReceiver = receiver.copy(balance = receiver.balance - tx.amount)
                            financeDao.updateWallet(updatedReceiver)
                        }
                    }
                }
            }
        }
    }

    suspend fun updateTransaction(newTx: TransactionEntity, oldTx: TransactionEntity) {
        // To update a transaction: first reverse the old transaction's impact, then apply the new one.
        deleteTransaction(oldTx)
        addTransaction(newTx.copy(id = oldTx.id))
    }

    // --- CATEGORY HANDLING ---
    suspend fun addCategory(category: CategoryEntity) = financeDao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = financeDao.updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity, reassignCategoryId: Long?) {
        if (reassignCategoryId != null) {
            val transactions = financeDao.getTransactionsByCategory(category.id)
            for (tx in transactions) {
                financeDao.insertTransaction(tx.copy(categoryId = reassignCategoryId))
            }
        }
        financeDao.deleteCategory(category)
    }

    // --- WALLET HANDLING ---
    suspend fun addWallet(wallet: WalletEntity) = financeDao.insertWallet(wallet)
    suspend fun updateWallet(wallet: WalletEntity) = financeDao.updateWallet(wallet)
    suspend fun deleteWallet(walletId: Long) = financeDao.deleteWalletById(walletId)

    // --- TRANSFER BETWEEN WALLETS ---
    suspend fun transferBetweenWallets(fromWalletId: Long, toWalletId: Long, amount: Double, date: Long, note: String) {
        val fromWallet = financeDao.getWalletById(fromWalletId)
        val toWallet = financeDao.getWalletById(toWalletId)
        if (fromWallet != null && toWallet != null) {
            val tx = TransactionEntity(
                amount = amount,
                type = "TRANSFER",
                categoryId = -1L, // Special transfer indicator or uncategorized
                walletId = fromWalletId,
                toWalletId = toWalletId,
                date = date,
                note = note,
                tags = "transfer",
                currency = fromWallet.currency,
                isRecurring = false
            )
            addTransaction(tx)
        }
    }

    // --- BUDGETS ---
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> = financeDao.getBudgetsForMonth(month, year)
    suspend fun saveBudget(budget: BudgetEntity) = financeDao.insertBudget(budget)

    // --- SAVINGS GOALS ---
    suspend fun addSavingsGoal(goal: SavingsGoalEntity) {
        financeDao.insertSavingsGoal(goal)
        unlockBadge("SAVINGS_STARTER")
    }
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        financeDao.updateSavingsGoal(goal)
        if (goal.currentAmount >= goal.targetAmount) {
            unlockBadge("GOAL_GETTER")
        }
    }
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = financeDao.deleteSavingsGoal(goal)

    // --- RECURRING TRANSACTIONS ---
    suspend fun addRecurringTransaction(recurring: RecurringTransactionEntity) = financeDao.insertRecurringTransaction(recurring)
    suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity) = financeDao.updateRecurringTransaction(recurring)
    suspend fun deleteRecurringTransaction(recurring: RecurringTransactionEntity) = financeDao.deleteRecurringTransaction(recurring)

    // --- LOANS ---
    suspend fun addLoan(loan: LoanEntity) = financeDao.insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = financeDao.updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = financeDao.deleteLoan(loan)

    // --- BILLS ---
    suspend fun addBill(bill: BillEntity) = financeDao.insertBill(bill)
    suspend fun updateBill(bill: BillEntity) = financeDao.updateBill(bill)
    suspend fun deleteBill(bill: BillEntity) = financeDao.deleteBill(bill)

    // --- EXCHANGE RATES ---
    suspend fun addExchangeRate(rate: ExchangeRateEntity) = financeDao.insertExchangeRate(rate)

    // --- BADGES ---
    suspend fun unlockBadge(badgeKey: String) {
        val existing = financeDao.getAllBadges().first()
        if (existing.none { it.badgeKey == badgeKey }) {
            financeDao.insertBadge(BadgeEntity(badgeKey = badgeKey))
        }
    }

    // --- DATA MANIPULATION ---
    suspend fun clearAllData() {
        // Custom db clear or reset tables
        // Simple destructive reload is fine or manual deletion of tables
    }

    // Import/Export integration helper
    suspend fun importBackup(transactions: List<TransactionEntity>, wallets: List<WalletEntity>) {
        for (w in wallets) {
            financeDao.insertWallet(w)
        }
        for (tx in transactions) {
            financeDao.insertTransaction(tx)
        }
    }
}
