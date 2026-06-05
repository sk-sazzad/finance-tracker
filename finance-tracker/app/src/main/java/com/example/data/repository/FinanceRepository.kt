package com.example.data.repository

import com.example.data.local.dao.FinanceDao
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Calendar

class FinanceRepository(private val context: android.content.Context, private val financeDao: FinanceDao, private val debtDao: com.example.data.local.dao.DebtDao) {

    // Streams of Data
    val allTransactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = financeDao.getAllCategories()
    val allWallets: Flow<List<WalletEntity>> = financeDao.getAllWallets()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = financeDao.getAllSavingsGoals()
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = financeDao.getAllRecurringTransactions()
    val allBills: Flow<List<BillEntity>> = financeDao.getAllBills()
    val allBadges: Flow<List<BadgeEntity>> = financeDao.getAllBadges()
    val allExchangeRates: Flow<List<ExchangeRateEntity>> = financeDao.getAllExchangeRates()
    
    val allDebtPersons: Flow<List<DebtPersonEntity>> = debtDao.getAllDebtPersons()
    val allDebtEntries: Flow<List<DebtEntryEntity>> = debtDao.getAllEntries()

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
                    checkBudgetWarning(tx.categoryId)
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

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        financeDao.deleteAllDepositsForGoal(goal.id)
        financeDao.deleteSavingsGoal(goal)
    }

    fun getDepositsForGoal(goalId: Long): Flow<List<SavingsDepositEntity>> =
        financeDao.getDepositsForGoal(goalId)

    suspend fun addSavingsDeposit(
        goalId: Long,
        amount: Double,
        walletId: Long?,
        note: String
    ) {
        // 1. Insert deposit record
        val deposit = SavingsDepositEntity(
            goalId = goalId,
            amount = amount,
            walletId = walletId,
            note = note
        )
        financeDao.insertSavingsDeposit(deposit)

        // 2. Update goal's currentAmount
        val goal = financeDao.getAllSavingsGoals().first().firstOrNull { it.id == goalId }
        if (goal != null) {
            val newAmount = goal.currentAmount + amount
            financeDao.updateSavingsGoal(goal.copy(currentAmount = newAmount))
            
            if (goal.currentAmount < goal.targetAmount && newAmount >= goal.targetAmount) {
                val appPrefs = com.example.data.local.preferences.UserPreferencesManager(context)
                if (appPrefs.notificationsEnabled.first()) {
                    com.example.notifications.NotificationHelper.sendGoalAchievedNotification(context, goalId, goal.name)
                }
            }
        }

        // 3. If walletId provided, deduct from wallet balance and log as EXPENSE transaction
        if (walletId != null) {
            val wallet = financeDao.getWalletById(walletId)
            if (wallet != null) {
                financeDao.updateWallet(wallet.copy(balance = wallet.balance - amount))
                val tx = TransactionEntity(
                    amount = amount,
                    type = "EXPENSE",
                    categoryId = -2L, // special savings category indicator
                    walletId = walletId,
                    date = System.currentTimeMillis(),
                    note = "Savings: ${goal?.name ?: "Goal"}",
                    tags = "savings",
                    currency = wallet.currency,
                    isRecurring = false
                )
                financeDao.insertTransaction(tx)
            }
        }

        // 4. Check if goal is now complete and unlock badge
        val updatedGoal = financeDao.getAllSavingsGoals().first().firstOrNull { it.id == goalId }
        if (updatedGoal != null && updatedGoal.currentAmount >= updatedGoal.targetAmount) {
            unlockBadge("GOAL_GETTER")
        }
    }

    private suspend fun checkBudgetWarning(categoryId: Long) {
        if (categoryId <= 0) return
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        val budgets = financeDao.getBudgetsForMonth(currentMonth, currentYear).first()
        val budget = budgets.find { it.categoryId == categoryId } ?: return
        
        val startCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        val txs = financeDao.getAllTransactions().first()
        val spent = txs.filter { 
            it.type == "EXPENSE" && 
            it.categoryId == categoryId && 
            it.date >= startCal.timeInMillis && 
            it.date <= endCal.timeInMillis 
        }.sumOf { it.amount }
        
        val percent = (spent / budget.amount) * 100
        if (percent >= 80) {
            val prefs = context.getSharedPreferences("budget_warnings", android.content.Context.MODE_PRIVATE)
            val key = "warn_80_${budget.id}_${currentMonth}_${currentYear}"
            if (!prefs.getBoolean(key, false)) {
                val category = financeDao.getAllCategories().first().find { it.id == categoryId }
                if (category != null) {
                    val appPrefs = com.example.data.local.preferences.UserPreferencesManager(context)
                    if (appPrefs.notificationsEnabled.first()) {
                        com.example.notifications.NotificationHelper.sendBudgetWarningNotification(context, categoryId, category.name, percent.toInt())
                    }
                }
                prefs.edit().putBoolean(key, true).apply()
            }
        }
    }

    // --- RECURRING TRANSACTIONS ---
    suspend fun addRecurringTransaction(recurring: RecurringTransactionEntity) = financeDao.insertRecurringTransaction(recurring)
    suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity) = financeDao.updateRecurringTransaction(recurring)
    suspend fun deleteRecurringTransaction(recurring: RecurringTransactionEntity) = financeDao.deleteRecurringTransaction(recurring)

    // --- BILLS ---
    suspend fun addBill(bill: BillEntity) {
        val id = financeDao.insertBill(bill)
        com.example.notifications.NotificationScheduler.scheduleBillReminder(context, id, bill.name, bill.amount, bill.dueDate)
    }
    suspend fun updateBill(bill: BillEntity) {
        financeDao.updateBill(bill)
        com.example.notifications.NotificationScheduler.scheduleBillReminder(context, bill.id, bill.name, bill.amount, bill.dueDate)
    }
    suspend fun deleteBill(bill: BillEntity) {
        financeDao.deleteBill(bill)
        com.example.notifications.NotificationScheduler.cancelBillReminder(context, bill.id)
    }

    // --- EXCHANGE RATES ---
    suspend fun addExchangeRate(rate: ExchangeRateEntity) = financeDao.insertExchangeRate(rate)

    // --- DEBTS ---
    suspend fun addDebtPerson(person: DebtPersonEntity): Long = debtDao.insertDebtPerson(person)
    
    suspend fun deleteDebtPerson(person: DebtPersonEntity) {
        debtDao.deleteAllEntriesForPerson(person.id)
        debtDao.deleteDebtPerson(person)
    }
    
    suspend fun addDebtEntry(entry: DebtEntryEntity) = debtDao.insertDebtEntry(entry)
    suspend fun updateDebtEntry(entry: DebtEntryEntity) = debtDao.updateDebtEntry(entry)
    suspend fun deleteDebtEntry(entry: DebtEntryEntity) = debtDao.deleteDebtEntry(entry)

    suspend fun payDebtEntry(entry: DebtEntryEntity, payAmount: Double) {
        val newPaid = (entry.paidAmount + payAmount).coerceAtMost(entry.amount)
        val newStatus = if (newPaid >= entry.amount) "PAID" else if (newPaid > 0) "PARTIALLY_PAID" else "UNPAID"
        updateDebtEntry(entry.copy(paidAmount = newPaid, status = newStatus))
    }

    suspend fun payDebtOverall(personId: Long, payAmount: Double) {
        var remainingAmount = payAmount
        val entries = debtDao.getEntriesForPerson(personId).first()
            .filter { it.status != "PAID" }
            .sortedBy { it.date }
            
        for (entry in entries) {
            if (remainingAmount <= 0) break
            val amountNeeded = entry.amount - entry.paidAmount
            if (amountNeeded <= 0) continue
            
            if (remainingAmount >= amountNeeded) {
                // Pay full
                remainingAmount -= amountNeeded
                updateDebtEntry(entry.copy(paidAmount = entry.amount, status = "PAID"))
            } else {
                // Pay partial
                val newPaid = entry.paidAmount + remainingAmount
                remainingAmount = 0.0
                updateDebtEntry(entry.copy(paidAmount = newPaid, status = "PARTIALLY_PAID"))
            }
        }
    }

    suspend fun payDebtOverallWithWallet(person: DebtPersonEntity, payAmount: Double, walletId: Long, markSettled: Boolean) {
        val personId = person.id
        var remainingAmount = payAmount
        val entries = debtDao.getEntriesForPerson(personId).first()
            .filter { it.status != "PAID" }
            .sortedBy { it.date }
            
        var totalAmountNeeded = 0.0
        for (entry in entries) {
            totalAmountNeeded += (entry.amount - entry.paidAmount)
        }
        val actualPayAmount = if (markSettled) totalAmountNeeded else remainingAmount.coerceAtMost(totalAmountNeeded)
        
        remainingAmount = actualPayAmount
        for (entry in entries) {
            if (remainingAmount <= 0) break
            val amountNeeded = entry.amount - entry.paidAmount
            if (amountNeeded <= 0) continue
            
            if (remainingAmount >= amountNeeded) {
                remainingAmount -= amountNeeded
                updateDebtEntry(entry.copy(paidAmount = entry.amount, status = "PAID"))
            } else {
                updateDebtEntry(entry.copy(paidAmount = entry.paidAmount + remainingAmount, status = "PARTIALLY_PAID"))
                remainingAmount = 0.0
            }
        }
        
        val wallet = financeDao.getWalletById(walletId)
        if (wallet != null && actualPayAmount > 0) {
            val isIncome = person.type == "LENT"
            val newBalance = if (isIncome) wallet.balance + actualPayAmount else wallet.balance - actualPayAmount
            financeDao.updateWallet(wallet.copy(balance = newBalance))
            
            val tx = com.example.data.local.entities.TransactionEntity(
                amount = actualPayAmount,
                type = if (isIncome) "INCOME" else "EXPENSE",
                categoryId = -3L,
                walletId = walletId,
                date = System.currentTimeMillis(),
                note = "Debt payment: ${person.personName}",
                tags = "debt,payment",
                currency = wallet.currency,
                isRecurring = false
            )
            financeDao.insertTransaction(tx)
        }
    }

    // --- BADGES ---
    suspend fun unlockBadge(badgeKey: String) {
        val existing = financeDao.getAllBadges().first()
        if (existing.none { it.badgeKey == badgeKey }) {
            financeDao.insertBadge(BadgeEntity(badgeKey = badgeKey))
        }
    }

    // --- DATA MANIPULATION ---
    suspend fun clearAllData() {
        financeDao.deleteAllTransactions()
        financeDao.deleteAllWallets()
        financeDao.deleteAllBudgets()
        financeDao.deleteAllSavingsGoals()
        financeDao.deleteAllSavingsDeposits()
        financeDao.deleteAllBills()
        financeDao.deleteAllBadges()
        debtDao.deleteAllDebtPersons()
        debtDao.deleteAllDebtEntries()
        // Re-populate defaults after clearing
        populateDefaultsIfEmpty()
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
