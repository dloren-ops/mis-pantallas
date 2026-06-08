package com.dloren.mispantallas.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dloren.mispantallas.data.local.AppDatabase
import com.dloren.mispantallas.data.mapper.toDomain
import com.dloren.mispantallas.data.notification.ReminderNotifier

/**
 * Worker que se ejecuta (aprox.) un día antes del vencimiento. Carga la cuenta,
 * verifica que siga vigente y muestra la notificación de recordatorio.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val accountId = inputData.getLong(KEY_ACCOUNT_ID, -1L)
        if (accountId < 0L) return Result.success()

        val dao = AppDatabase.getInstance(applicationContext).accountDao()
        val account = dao.getById(accountId)?.toDomain() ?: return Result.success()

        // Si ya venció (o se cambió la fecha), no molestamos.
        if (account.isExpired()) return Result.success()

        ReminderNotifier.show(applicationContext, account)
        return Result.success()
    }

    companion object {
        const val KEY_ACCOUNT_ID = "accountId"
    }
}
