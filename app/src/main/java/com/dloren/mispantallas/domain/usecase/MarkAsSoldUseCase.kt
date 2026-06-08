package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AccountStatus
import com.dloren.mispantallas.domain.reminder.ReminderScheduler
import com.dloren.mispantallas.domain.repository.AccountRepository

/**
 * Marca una cuenta como **vendida**: fija la fecha de venta = ahora (arranca el
 * conteo de los días de alquiler) y reprograma el recordatorio del cliente.
 * Si ya estaba vendida, no hace nada.
 */
class MarkAsSoldUseCase(
    private val repository: AccountRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(account: Account): Account {
        if (account.status == AccountStatus.SOLD) return account
        val sold = account.copy(
            status = AccountStatus.SOLD,
            soldDateMillis = System.currentTimeMillis()
        )
        repository.saveAccount(sold)
        reminderScheduler.schedule(sold)
        return sold
    }
}
