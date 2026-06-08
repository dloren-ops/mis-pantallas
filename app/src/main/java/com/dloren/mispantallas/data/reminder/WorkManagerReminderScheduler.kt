package com.dloren.mispantallas.data.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.reminder.ReminderScheduler
import java.util.concurrent.TimeUnit

/**
 * Programa el recordatorio con WorkManager: encola un trabajo único por cuenta
 * que se dispara aproximadamente un día antes del vencimiento. Sobrevive a
 * reinicios del dispositivo.
 */
class WorkManagerReminderScheduler(context: Context) : ReminderScheduler {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedule(account: Account) {
        val now = System.currentTimeMillis()

        // Si ya venció, no hay nada que recordar.
        if (account.endDateMillis <= now) {
            cancel(account.id)
            return
        }

        val triggerAt = account.endDateMillis - TimeUnit.DAYS.toMillis(1)
        val delayMillis = (triggerAt - now).coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_ACCOUNT_ID to account.id))
            .build()

        workManager.enqueueUniqueWork(
            workName(account.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel(accountId: Long) {
        workManager.cancelUniqueWork(workName(accountId))
    }

    private fun workName(accountId: Long) = "reminder_$accountId"
}
