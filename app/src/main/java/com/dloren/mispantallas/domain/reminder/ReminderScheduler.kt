package com.dloren.mispantallas.domain.reminder

import com.dloren.mispantallas.domain.model.Account

/**
 * Contrato para programar/cancelar el recordatorio de vencimiento de una cuenta
 * (un día antes de que venza). La implementación concreta vive en la capa de
 * datos (WorkManager).
 */
interface ReminderScheduler {
    /** Programa (o reprograma) el recordatorio para la cuenta dada. */
    fun schedule(account: Account)

    /** Cancela el recordatorio de la cuenta indicada. */
    fun cancel(accountId: Long)
}
