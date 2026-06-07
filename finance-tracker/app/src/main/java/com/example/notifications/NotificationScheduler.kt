package com.example.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.data.local.db.FinanceDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleDailySummary(context: Context) {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        calendar.set(Calendar.HOUR_OF_DAY, 21) // 9 PM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = calendar.timeInMillis - now

        val constraints = Constraints.Builder().build()
        val workRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailySummaryWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleBillReminder(context: Context, billId: Long, billName: String, amount: Double, dueDate: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Intent for 3 days before
        val intent3Days = Intent(context, BillReminderReceiver::class.java).apply {
            putExtra("billId", billId)
            putExtra("billName", billName)
            putExtra("amount", amount)
            putExtra("daysLeft", 3)
        }
        val pendingIntent3Days = PendingIntent.getBroadcast(
            context,
            (billId * 10).toInt(), // unique request code
            intent3Days,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for due date (today)
        val intentToday = Intent(context, BillReminderReceiver::class.java).apply {
            putExtra("billId", billId)
            putExtra("billName", billName)
            putExtra("amount", amount)
            putExtra("daysLeft", 0)
        }
        val pendingIntentToday = PendingIntent.getBroadcast(
            context,
            (billId * 10 + 1).toInt(), // unique request code
            intentToday,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calDue = Calendar.getInstance().apply { timeInMillis = dueDate }
        calDue.set(Calendar.HOUR_OF_DAY, 9) // Notify at 9 AM
        calDue.set(Calendar.MINUTE, 0)
        calDue.set(Calendar.SECOND, 0)
        
        val timeToday = calDue.timeInMillis
        val time3Days = timeToday - TimeUnit.DAYS.toMillis(3)
        val now = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        if (time3Days > now) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time3Days, pendingIntent3Days)
        }
        if (timeToday > now) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeToday, pendingIntentToday)
        }
    }

    fun cancelBillReminder(context: Context, billId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent3Days = Intent(context, BillReminderReceiver::class.java)
        val pendingIntent3Days = PendingIntent.getBroadcast(
            context, (billId * 10).toInt(), intent3Days, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent3Days)

        val intentToday = Intent(context, BillReminderReceiver::class.java)
        val pendingIntentToday = PendingIntent.getBroadcast(
            context, (billId * 10 + 1).toInt(), intentToday, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntentToday)
    }
}

class BillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = com.example.data.local.preferences.UserPreferencesManager(context)
        if (!runBlocking { prefs.notificationsEnabled.first() }) return

        val billId = intent.getLongExtra("billId", -1L)
        val billName = intent.getStringExtra("billName") ?: return
        val amount = intent.getDoubleExtra("amount", 0.0)
        val daysLeft = intent.getIntExtra("daysLeft", 0)

        if (billId == -1L) return

        // Verify it isn't already paid this month
        runBlocking {
            val db = FinanceDatabase.getDatabase(context)
            val financeDao = db.financeDao()
            val bills = financeDao.getAllBills().first()
            val bill = bills.firstOrNull { it.id == billId }
            if (bill != null) {
                val currentCal = Calendar.getInstance()
                val isPaidThisMonth = bill.lastPaid != null && 
                    Calendar.getInstance().apply { timeInMillis = bill.lastPaid }.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH) && 
                    Calendar.getInstance().apply { timeInMillis = bill.lastPaid }.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)
                
                if (!isPaidThisMonth) {
                    NotificationHelper.sendBillDueNotification(context, billId, billName, amount, daysLeft)
                }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            runBlocking {
                val db = FinanceDatabase.getDatabase(context)
                val financeDao = db.financeDao()
                val bills = financeDao.getAllBills().first()
                for (bill in bills) {
                    if (bill.dueDate > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) {
                        NotificationScheduler.scheduleBillReminder(context, bill.id, bill.name, bill.amount, bill.dueDate)
                    }
                }
            }
            NotificationScheduler.scheduleDailySummary(context)
        }
    }
}

class DailySummaryWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val prefs = com.example.data.local.preferences.UserPreferencesManager(context)
        if (!runBlocking { prefs.notificationsEnabled.first() }) return Result.success()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        val startOfDay = cal.timeInMillis

        return runBlocking {
            val db = FinanceDatabase.getDatabase(context)
            val financeDao = db.financeDao()
            val txs = financeDao.getAllTransactions().first()
            
            val todaysTxs = txs.filter { it.date >= startOfDay }
            val todaysExpenses = todaysTxs.filter { it.type == "EXPENSE" }

            if (todaysTxs.isNotEmpty() && todaysExpenses.isNotEmpty()) {
                val totalExpense = todaysExpenses.sumOf { it.amount }
                NotificationHelper.sendDailySummaryNotification(context, totalExpense, todaysTxs.size)
            }
            Result.success()
        }
    }
}
