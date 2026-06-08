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
 * Programa los recordatorios con WorkManager. Por cada cuenta puede haber dos:
 *  - **Cliente**: ~1 día antes del vencimiento de la venta (si está vendida).
 *  - **Proveedor**: ~1 día antes de tu renovación (si renewEveryDays > 0).
 *
 * Son trabajos únicos por cuenta y sobreviven a reinicios del dispositivo.
 */
class WorkManagerReminderScheduler(context: Context) : ReminderScheduler {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedule(account: Account) {
        scheduleClient(account)
        scheduleProvider(account)
    }

    private fun scheduleClient(account: Account) {
        val now = System.currentTimeMillis()
        if (!account.isSold || account.clientEndDateMillis <= now) {
            workManager.cancelUniqueWork(clientWork(account.id))
            return
        }
        val triggerAt = account.clientEndDateMillis - TimeUnit.DAYS.toMillis(1)
        enqueue(clientWork(account.id), account.id, ReminderWorker.TYPE_CLIENT, triggerAt - now)
    }

    private fun scheduleProvider(account: Account) {
        val now = System.currentTimeMillis()
        if (!account.hasProviderRenewal || account.providerRenewMillis <= now) {
            workManager.cancelUniqueWork(providerWork(account.id))
            return
        }
        val triggerAt = account.providerRenewMillis - TimeUnit.DAYS.toMillis(1)
        enqueue(providerWork(account.id), account.id, ReminderWorker.TYPE_PROVIDER, triggerAt - now)
    }

    private fun enqueue(workName: String, accountId: Long, type: String, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_ACCOUNT_ID to accountId,
                    ReminderWorker.KEY_TYPE to type
                )
            )
            .build()
        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(accountId: Long) {
        workManager.cancelUniqueWork(clientWork(accountId))
        workManager.cancelUniqueWork(providerWork(accountId))
    }

    private fun clientWork(accountId: Long) = "reminder_client_$accountId"
    private fun providerWork(accountId: Long) = "reminder_provider_$accountId"
}
