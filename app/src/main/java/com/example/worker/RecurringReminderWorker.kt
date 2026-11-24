package com.example.worker

import android.Manifest
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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.RupeeLedgerApp
import com.example.data.repository.toInput
import com.example.domain.finance.DashboardCalculator
import com.example.domain.finance.DateRangeUtils
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class RecurringReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RupeeLedgerApp ?: return Result.success()
        val prefs = app.container.preferencesRepository.preferencesFlow.first()
        if (!prefs.notificationsEnabled) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        ensureChannel(applicationContext)

        val todayEpoch = LocalDate.now().toEpochDay()
        val recurring = app.container.financeRepository.recurringPaymentsFlow.first()
        val dueItems = recurring.filter { item ->
            item.isActive &&
                item.reminderEnabled &&
                (item.nextDueDateEpochDay - todayEpoch) <= item.reminderDaysBefore.toLong()
        }

        if (dueItems.isNotEmpty()) {
            val first = dueItems.first()
            val title = if (dueItems.size == 1) {
                "Bill due: ${first.title}"
            } else {
                "${dueItems.size} recurring bills due soon"
            }
            val body = if (dueItems.size == 1) {
                "${MoneyFormatter.format(first.amountPaise)} • Tap to review and confirm payment"
            } else {
                "Next: ${first.title} (${MoneyFormatter.format(first.amountPaise)}). Tap to review."
            }
            postNotification(applicationContext, NOTIF_ID_RECURRING, title, body)
        }

        if (prefs.budgetAlertsEnabled) {
            val budgets = app.container.financeRepository.budgetsFlow.first().filter { it.isActive }
            val transactions = app.container.financeRepository.transactionsFlow.first()
            val today = LocalDate.now()
            val alerted = budgets.mapNotNull { budget ->
                val period = runCatching { BudgetPeriod.valueOf(budget.periodType) }
                    .getOrDefault(BudgetPeriod.MONTHLY)
                val (startDate, endDate) = DateRangeUtils.budgetPeriodDates(
                    period = period,
                    today = today,
                    customStartEpochDay = budget.customStartEpochDay,
                    customEndEpochDay = budget.customEndEpochDay,
                )
                val (startMs, endMs) = DateRangeUtils.toEpochMillisRange(startDate, endDate)
                val periodTx = transactions.filter { it.timestamp in startMs..endMs }
                    .map { it.toInput() }
                val util = DashboardCalculator.calculateBudgetUtilization(
                    budgetId = budget.id,
                    name = budget.name,
                    categoryId = budget.categoryId,
                    limitPaise = budget.limitAmountPaise,
                    alertThresholdPercent = budget.alertThresholdPercent,
                    periodTransactions = periodTx,
                )
                if (util.status != BudgetStatus.NORMAL) util else null
            }
            if (alerted.isNotEmpty()) {
                val top = alerted.maxBy { it.utilizationPercent }
                val msg = if (top.status == BudgetStatus.EXCEEDED) {
                    "${top.name} exceeded (${top.utilizationPercent}% used)"
                } else {
                    "${top.name} at ${top.utilizationPercent}% of limit"
                }
                postNotification(applicationContext, NOTIF_ID_BUDGET, "Budget Alert", msg)
            }
        }

        return Result.success()
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun postNotification(context: Context, id: Int, title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "rupee_ledger_reminders"
        private const val WORK_NAME = "rupee_ledger_daily_reminders"
        private const val NOTIF_ID_RECURRING = 1001
        private const val NOTIF_ID_BUDGET = 1002

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
