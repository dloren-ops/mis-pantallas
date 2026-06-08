package com.dloren.mispantallas.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dloren.mispantallas.data.local.AppDatabase
import com.dloren.mispantallas.data.mapper.toDomain
import com.dloren.mispantallas.data.notification.ReminderNotifier

/**
 * Worker que se ejecuta (aprox.) un día antes de un vencimiento. Según el tipo,
 * muestra el recordatorio del cliente (para renovar la venta) o el propio
 * (para que vos renueves con el proveedor).
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val accountId = inputData.getLong(KEY_ACCOUNT_ID, -1L)
        if (accountId < 0L) return Result.success()
        val type = inputData.getString(KEY_TYPE) ?: TYPE_CLIENT

        val dao = AppDatabase.getInstance(applicationContext).accountDao()
        val account = dao.getById(accountId)?.toDomain() ?: return Result.success()

        when (type) {
            TYPE_PROVIDER -> {
                if (account.hasProviderRenewal) {
                    ReminderNotifier.showProviderReminder(applicationContext, account)
                }
            }
            else -> {
                if (account.isSold && !account.isClientExpired()) {
                    ReminderNotifier.showClientReminder(applicationContext, account)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_ACCOUNT_ID = "accountId"
        const val KEY_TYPE = "type"
        const val TYPE_CLIENT = "client"
        const val TYPE_PROVIDER = "provider"
    }
}
