package com.dloren.mispantallas.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dloren.mispantallas.data.AppDatabase
import com.dloren.mispantallas.data.AppSettingsPreferences
import com.dloren.mispantallas.util.NotificationHelper
import kotlinx.coroutines.flow.first

/** Revisa una vez al día las pantallas alquiladas y avisa si alguna vence pronto o ya venció. */
class RenewalReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val accounts = AppDatabase.getInstance(applicationContext).accountDao().observeAll().first()
        val thresholdDays = AppSettingsPreferences(applicationContext).reminderDays.first()
        val expiring = accounts.filter { account ->
            if (account.clientPhone.isBlank()) return@filter false
            val clientDue = account.remainingDays() <= thresholdDays
            val providerDue = (account.providerRemainingDays() ?: Long.MAX_VALUE) <= thresholdDays
            clientDue || providerDue
        }
        NotificationHelper.notifyExpiring(applicationContext, expiring)
        return Result.success()
    }
}
