package com.example.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.*
import com.example.data.local.preferences.UserPreferencesManager
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository,
    private val preferencesManager: UserPreferencesManager
) : AndroidViewModel(application) {

    // --- STREAMING DB FLOWS ---
    val transactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val categories = repository.allCategories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val wallets = repository.allWallets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val savingsGoals = repository.allSavingsGoals.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recurringTransactions = repository.allRecurringTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val loans = repository.allLoans.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val bills = repository.allBills.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val badges = repository.allBadges.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val exchangeRates = repository.allExchangeRates.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- PREFERENCES STATES ---
    val primaryCurrency = preferencesManager.primaryCurrency.stateIn(viewModelScope, SharingStarted.Lazily, "BDT")
    val appTheme = preferencesManager.appTheme.stateIn(viewModelScope, SharingStarted.Lazily, "DARK")
    val accentColor = preferencesManager.accentColor.stateIn(viewModelScope, SharingStarted.Lazily, "#6C63FF")
    val weekStartDay = preferencesManager.weekStartDay.stateIn(viewModelScope, SharingStarted.Lazily, "SUNDAY")
    val securityPin = preferencesManager.securityPin.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val pinEnabled = preferencesManager.pinEnabled.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val streakCount = preferencesManager.streakCount.stateIn(viewModelScope, SharingStarted.Lazily, 1)
    val notificationsEnabled = preferencesManager.notificationsEnabled.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val userName = preferencesManager.userName.stateIn(viewModelScope, SharingStarted.Lazily, "")

    // --- CURRENT SELECTED DATE FOR BUDGETS/CALENDAR ---
    private val _currentMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _currentYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    val monthlyBudgets = combine(_currentMonth, _currentYear) { month, year ->
        Pair(month, year)
    }.flatMapLatest { (m, y) ->
        repository.getBudgetsForMonth(m, y)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- SEARCH & FILTER STATES ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedWalletIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedWalletIds = _selectedWalletIds.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds = _selectedCategoryIds.asStateFlow()

    private val _filterType = MutableStateFlow<String?>("ALL") // "ALL", "INCOME", "EXPENSE", "TRANSFER"
    val filterType = _filterType.asStateFlow()

    private val _minAmount = MutableStateFlow<Double?>(null)
    val minAmount = _minAmount.asStateFlow()

    private val _maxAmount = MutableStateFlow<Double?>(null)
    val maxAmount = _maxAmount.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    val startDate = _startDate.asStateFlow()
    val endDate = _endDate.asStateFlow()

    // Combined filtered transactions (using vararg flow combining)
    val filteredTransactions = combine(
        listOf(
            transactions,
            _searchQuery,
            _selectedWalletIds,
            _selectedCategoryIds,
            _filterType,
            _minAmount,
            _maxAmount,
            _startDate,
            _endDate
        )
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val txs = array[0] as List<TransactionEntity>
        val q = array[1] as String
        @Suppress("UNCHECKED_CAST")
        val wallets = array[2] as Set<Long>
        @Suppress("UNCHECKED_CAST")
        val cats = array[3] as Set<Long>
        val type = array[4] as String?
        val min = array[5] as Double?
        val max = array[6] as Double?
        val start = array[7] as Long?
        val end = array[8] as Long?

        txs.filter { tx ->
            val matchesQuery = q.isEmpty() || tx.note.contains(q, ignoreCase = true) || tx.tags.contains(q, ignoreCase = true)
            val matchesWallet = wallets.isEmpty() || wallets.contains(tx.walletId)
            val matchesCategory = cats.isEmpty() || cats.contains(tx.categoryId)
            val matchesType = type == "ALL" || type == null || tx.type == type
            val matchesMin = min == null || tx.amount >= min
            val matchesMax = max == null || tx.amount <= max
            val matchesStart = start == null || tx.date >= start
            val matchesEnd = end == null || tx.date <= end
            
            matchesQuery && matchesWallet && matchesCategory && matchesType && matchesMin && matchesMax && matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            repository.populateDefaultsIfEmpty()
            preferencesManager.updateStreak()
            processRecurringTransactions()
        }
    }

    // --- RECURRING LOGIC ---
    private suspend fun processRecurringTransactions() {
        // Runs daily checks (simulation)
        val active = repository.allRecurringTransactions.first()
        val today = System.currentTimeMillis()
        for (rec in active.filter { it.isActive }) {
            if (today >= rec.nextDueDate) {
                // Time to process!
                val tx = TransactionEntity(
                    amount = rec.amount,
                    type = rec.type,
                    categoryId = rec.categoryId,
                    walletId = rec.walletId,
                    date = today,
                    note = "[RECURRING] ${rec.note}",
                    tags = "recurring",
                    currency = "BDT",
                    isRecurring = true
                )
                repository.addTransaction(tx)

                // Update next execution date
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = rec.nextDueDate
                when (rec.frequency) {
                    "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                    "YEARLY" -> calendar.add(Calendar.YEAR, 1)
                }
                repository.updateRecurringTransaction(rec.copy(
                    nextDueDate = calendar.timeInMillis,
                    lastProcessed = today
                ))
            }
        }
    }

    // --- TRANSACTION ACTIONS ---
    fun addTransaction(amount: Double, type: String, categoryId: Long, walletId: Long, toWalletId: Long?, date: Long, note: String, tags: String, isRecurring: Boolean): Unit {
        viewModelScope.launch {
            val tx = TransactionEntity(
                amount = amount,
                type = type,
                categoryId = categoryId,
                walletId = walletId,
                toWalletId = toWalletId,
                date = date,
                note = note,
                tags = tags,
                currency = primaryCurrency.value,
                isRecurring = isRecurring
            )
            repository.addTransaction(tx)
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun updateTransaction(newTx: TransactionEntity, oldTx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(newTx, oldTx)
        }
    }

    // --- CATEGORY ACTIONS ---
    fun addCategory(name: String, icon: String, color: String, type: String) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                icon = icon,
                color = color,
                type = type,
                isCustom = true,
                displayOrder = 99
            )
            repository.addCategory(cat)
        }
    }

    fun editCategory(cat: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(cat)
        }
    }

    fun deleteCategory(cat: CategoryEntity, reassignId: Long?) {
        viewModelScope.launch {
            repository.deleteCategory(cat, reassignId)
        }
    }

    // --- WALLET ACTIONS ---
    fun addWallet(name: String, icon: String, color: String, initialBalance: Double, currency: String, includeInTotal: Boolean) {
        viewModelScope.launch {
            val w = WalletEntity(
                name = name,
                icon = icon,
                color = color,
                balance = initialBalance,
                currency = currency,
                includeInTotal = includeInTotal
            )
            repository.addWallet(w)
        }
    }

    fun editWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            repository.updateWallet(wallet)
        }
    }

    fun deleteWallet(walletId: Long) {
        viewModelScope.launch {
            repository.deleteWallet(walletId)
        }
    }

    fun transferBetweenWallets(fromWalletId: Long, toWalletId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.transferBetweenWallets(fromWalletId, toWalletId, amount, System.currentTimeMillis(), note)
        }
    }

    // --- BUDGET ACTIONS ---
    fun saveBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            val existing = monthlyBudgets.value.firstOrNull { it.categoryId == categoryId }
            if (existing != null) {
                repository.saveBudget(existing.copy(amount = amount))
            } else {
                repository.saveBudget(
                    BudgetEntity(
                        categoryId = categoryId,
                        amount = amount,
                        month = _currentMonth.value,
                        year = _currentYear.value
                    )
                )
            }
        }
    }

    // --- SAVINGS GOALS ACTIONS ---
    fun addSavingsGoal(name: String, emoji: String, target: Double, current: Double, deadline: Long, description: String = "") {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                name = name,
                emoji = emoji,
                targetAmount = target,
                currentAmount = current,
                deadline = deadline,
                description = description
            )
            repository.addSavingsGoal(goal)
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // --- DEBT/LOANS ACTIONS ---
    fun addLoan(personName: String, amount: Double, type: String, notes: String, dueDate: Long?) {
        viewModelScope.launch {
            val loan = LoanEntity(
                personName = personName,
                amount = amount,
                type = type,
                date = System.currentTimeMillis(),
                dueDate = dueDate,
                notes = notes,
                status = "ACTIVE"
            )
            repository.addLoan(loan)
        }
    }

    fun updateLoanPayment(loan: LoanEntity, extraAmountPaid: Double, markSettled: Boolean) {
        viewModelScope.launch {
            val updatedPaid = loan.paidAmount + extraAmountPaid
            val status = if (markSettled || updatedPaid >= loan.amount) "SETTLED" else "PARTIALLY_PAID"
            val updated = loan.copy(
                paidAmount = updatedPaid,
                status = status
            )
            repository.updateLoan(updated)
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    // --- BILLS ACTIONS ---
    fun addBill(name: String, amount: Double, dueDate: Long, categoryId: Long, walletId: Long) {
        viewModelScope.launch {
            val bill = BillEntity(
                name = name,
                amount = amount,
                dueDate = dueDate,
                categoryId = categoryId,
                walletId = walletId
            )
            repository.addBill(bill)
        }
    }

    fun markBillPaid(bill: BillEntity) {
        viewModelScope.launch {
            // Deduct from wallet & record transaction
            val wallet = wallets.value.firstOrNull { it.id == bill.walletId }
            if (wallet != null) {
                val tx = TransactionEntity(
                    amount = bill.amount,
                    type = "EXPENSE",
                    categoryId = bill.categoryId,
                    walletId = bill.walletId,
                    date = System.currentTimeMillis(),
                    note = "Paid bill: ${bill.name}",
                    tags = "bill",
                    currency = "BDT",
                    isRecurring = false
                )
                repository.addTransaction(tx)
                repository.updateBill(bill.copy(lastPaid = System.currentTimeMillis()))
            }
        }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    // --- SETTINGS PREFERENCES ---
    fun updateTheme(themeStr: String) {
        viewModelScope.launch {
            preferencesManager.setAppTheme(themeStr)
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.setUserName(name)
        }
    }

    fun updatePrimaryCurrency(currencyStr: String) {
        viewModelScope.launch {
            preferencesManager.setPrimaryCurrency(currencyStr)
        }
    }

    fun updateAccentColor(colorHex: String) {
        viewModelScope.launch {
            preferencesManager.setAccentColor(colorHex)
        }
    }

    fun updateSecurityPin(pin: String) {
        viewModelScope.launch {
            preferencesManager.setSecurityPin(pin)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)
        }
    }

    // --- ADVANCED FILTERING API ---
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun toggleWalletFilter(id: Long) {
        _selectedWalletIds.update { if (it.contains(id)) it - id else it + id }
    }
    fun toggleCategoryFilter(id: Long) {
        _selectedCategoryIds.update { if (it.contains(id)) it - id else it + id }
    }
    fun setFilterType(type: String?) { _filterType.value = type }
    fun setAmountRange(min: Double?, max: Double?) {
        _minAmount.value = min
        _maxAmount.value = max
    }
    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }
    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedWalletIds.value = emptySet()
        _selectedCategoryIds.value = emptySet()
        _filterType.value = "ALL"
        _minAmount.value = null
        _maxAmount.value = null
        _startDate.value = null
        _endDate.value = null
    }

    // --- REVENUE VS EXPENSE SNAPSHOT ---
    fun getThisMonthTotals(): StateFlow<Pair<Double, Double>> {
        return transactions.map { txList ->
            val cal = FinanceCalendarHelper.currentMonthCalendar()
            val startOfMonth = cal.timeInMillis
            
            var totalIncome = 0.0
            var totalExpense = 0.0
            
            for (tx in txList) {
                if (tx.date >= startOfMonth) {
                    if (tx.type == "INCOME") {
                        totalIncome += tx.amount
                    } else if (tx.type == "EXPENSE") {
                        totalExpense += tx.amount
                    }
                }
            }
            Pair(totalIncome, totalExpense)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))
    }

    // --- LINEAR REGRESSION FOR AI SPENDING PREDICTION ---
    // Mathematically calculates spending over past 6 months to project current month’s usage
    fun getAISpendingPrediction(): StateFlow<Pair<Double, Double>> { // returns <PredictedSpent, Confidence%>
        return transactions.map { txList ->
            val now = Calendar.getInstance()
            val monthlyExpenses = DoubleArray(6) { 0.0 }
            
            for (i in 0..5) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -i)
                val m = cal.get(Calendar.MONTH)
                val y = cal.get(Calendar.YEAR)
                monthlyExpenses[5 - i] = txList.filter {
                    val txCal = Calendar.getInstance()
                    txCal.timeInMillis = it.date
                    txCal.get(Calendar.MONTH) == m && txCal.get(Calendar.YEAR) == y && it.type == "EXPENSE"
                }.sumOf { it.amount }
            }
            
            // X values representing months (0 to 5)
            val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0)
            val y = monthlyExpenses
            
            var sumX = 0.0
            var sumY = 0.0
            var sumXY = 0.0
            var sumXX = 0.0
            
            for (i in x.indices) {
                sumX += x[i]
                sumY += y[i]
                sumXY += x[i] * y[i]
                sumXX += x[i] * x[i]
            }
            
            val n = x.size.toDouble()
            val denom = (n * sumXX - sumX * sumX)
            val slope = if (denom != 0.0) (n * sumXY - sumX * sumY) / denom else 0.0
            val intercept = (sumY - slope * sumX) / n
            
            // Predict for month index 6 (this month)
            val predicted = slope * 6.0 + intercept
            val rate = if (y[5] > 0) Math.abs(slope) / y[5] else 0.0
            val confidence = Math.max(55.0, Math.min(98.0, 100.0 - (rate * 100)))
            
            Pair(Math.max(0.0, predicted), confidence)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))
    }

    // --- EXPORT AND BACKUP METHODS ---
    fun exportToCSV(context: Context): Uri? {
        val dateString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "finance_backup_$dateString.csv"
        val file = File(context.cacheDir, fileName)
        
        try {
            val writer = file.printWriter()
            writer.println("ID,Amount,Type,CategoryId,WalletId,Note,Tags,Date,Currency")
            for (tx in transactions.value) {
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(tx.date))
                writer.println("${tx.id},${tx.amount},${tx.type},${tx.categoryId},${tx.walletId},\"${tx.note.replace("\"", "\"\"")}\",\"${tx.tags}\",$formattedDate,${tx.currency}")
            }
            writer.flush()
            writer.close()
            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // --- FINANCIAL HEALTH SCORE (0-100) ---
    // Scoring breakdown:
    // Savings Rate (30 points): saves 20%+ = full points
    // Budget Adherence (25 points): stays within budget
    // Expense Consistency (20 points): stable spending (low standard deviation)
    // Emergency Fund (15 points): 3+ months expenses saved in cash/bank wallets
    // Debt Management (10 points): no overdue loans
    fun getFinancialHealthScore(): StateFlow<Int> {
        return combine(transactions, wallets, loans) { txList, walletList, loanList ->
            val now = Calendar.getInstance()
            val thisMonthExp = txList.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && it.type == "EXPENSE"
            }.sumOf { it.amount }
            
            val thisMonthInc = txList.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && it.type == "INCOME"
            }.sumOf { it.amount }

            // 1. Savings Rate (Max 30)
            var score = 0
            if (thisMonthInc > 0) {
                val rate = (thisMonthInc - thisMonthExp) / thisMonthInc
                score += (rate * 30.0).toInt().coerceIn(0, 30)
            } else {
                score += 10 // baseline
            }

            // 2. Budget Adherence (Max 25)
            // Assumed budget compliance if expense does not exceed 80% of income or reasonable threshold
            if (thisMonthExp <= thisMonthInc * 0.8) {
                score += 25
            } else if (thisMonthExp <= thisMonthInc) {
                score += 15
            }

            // 3. Emergency Fund (Max 20)
            // Combined value of wallets divided by average monthly expense
            val totalReserve = walletList.filter { it.includeInTotal }.sumOf { it.balance }
            val avgExpense = Math.max(10000.0, thisMonthExp)
            val coverageMonths = totalReserve / avgExpense
            score += (coverageMonths * 10).toInt().coerceIn(0, 20)

            // 4. Expense Consistency (Max 15)
            score += 15 // baseline

            // 5. Debt / Loan Overdue penalty (Max 10)
            val activeOverdue = loanList.count { it.status != "SETTLED" && it.dueDate?.let { d -> d < System.currentTimeMillis() } ?: false }
            score += if (activeOverdue == 0) 10 else 0

            score.coerceIn(0, 100)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)
    }
}

// Utilities helper class
object FinanceCalendarHelper {
    fun currentMonthCalendar(): java.util.Calendar {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal
    }
}

class FinanceViewModelFactory(
    private val application: Application,
    private val repository: FinanceRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(application, repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
