package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.R

object NotificationHelper {

    private const val CHANNEL_ID = "finance_tracker_channel"
    private const val CHANNEL_NAME = "Finance Alerts"

    // Unique IDs for different notification types
    private const val BILL_REMINDER_BASE_ID = 1000 // We'll add bill ID to this
    private const val BUDGET_WARNING_BASE_ID = 2000 // We'll add category ID to this
    private const val RECURRING_PROCESSED_ID = 3000
    private const val GOAL_ACHIEVED_BASE_ID = 4000 // We'll add goal ID to this
    private const val DAILY_SUMMARY_ID = 5000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for bills, budgets, and summaries"
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getMainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun sendNotification(context: Context, id: Int, title: String, message: String) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        // Check user preference
        val prefs = com.example.data.local.preferences.UserPreferencesManager(context)
        val enabled = kotlinx.coroutines.runBlocking {
            prefs.notificationsEnabled.first()
        }
        if (!enabled) return

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getMainActivityPendingIntent(context))
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 100, 500))

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }

    fun sendBillDueNotification(context: Context, billId: Long, billName: String, amount: Double, daysLeft: Int) {
        val title = "Bill Due Soon 💡"
        val message = if (daysLeft == 0) {
            "${billName} of ৳${String.format("%,.0f", amount)} is due today!"
        } else {
            "${billName} of ৳${String.format("%,.0f", amount)} is due in $daysLeft days"
        }
        sendNotification(context, BILL_REMINDER_BASE_ID + billId.toInt(), title, message)
    }

    fun sendBudgetWarningNotification(context: Context, categoryId: Long, categoryName: String, percentUsed: Int) {
        val title = "Budget Alert ⚠️"
        val message = "You've used $percentUsed% of your $categoryName budget"
        sendNotification(context, BUDGET_WARNING_BASE_ID + categoryId.toInt(), title, message)
    }

    fun sendRecurringProcessedNotification(context: Context, amount: Double, note: String) {
        val title = "Auto Payment Done 🔁"
        val message = "৳${String.format("%,.0f", amount)} was auto-deducted for $note"
        // Using a random or incrementing ID if we want them to stack, or just a fixed one to update. 
        // We'll use a fixed ID so they overwrite each other for simplicity, or timestamp.
        sendNotification(context, RECURRING_PROCESSED_ID + System.currentTimeMillis().toInt(), title, message)
    }

    fun sendGoalAchievedNotification(context: Context, goalId: Long, goalName: String) {
        val title = "Goal Achieved! \uD83C\uDF89" // 🎉
        val message = "You've completed your savings goal: $goalName"
        sendNotification(context, GOAL_ACHIEVED_BASE_ID + goalId.toInt(), title, message)
    }

    fun sendDailySummaryNotification(context: Context, totalExpense: Double, transactionCount: Int) {
        val title = "Today's Summary \uD83D\uDCCA" // 📊
        val message = "You spent ৳${String.format("%,.0f", totalExpense)} today across $transactionCount transactions"
        sendNotification(context, DAILY_SUMMARY_ID, title, message)
    }
}
