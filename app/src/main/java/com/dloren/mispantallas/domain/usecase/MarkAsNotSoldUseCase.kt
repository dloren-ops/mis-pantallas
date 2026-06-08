package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AccountStatus
import com.dloren.mispantallas.domain.reminder.ReminderScheduler
import com.dloren.mispantallas.domain.repository.AccountRepository

/**
 * Devuelve una cuenta a estado **no vendida**: borra la fecha de venta y cancela
 * el recordatorio del cliente (la cuenta vuelve a la pantalla "Nuevas").
 */
class MarkAsNotSoldUseCase(
    private val repository: AccountRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(account: Account): Account {
        val updated = account.copy(
            status = AccountStatus.NOT_SOLD,
            soldDateMillis = 0L
        )
        repository.saveAccount(updated)
        reminderScheduler.schedule(updated)
        return updated
    }
}
