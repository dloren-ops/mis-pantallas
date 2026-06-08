package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.reminder.ReminderScheduler
import com.dloren.mispantallas.domain.repository.AccountRepository

/**
 * Guarda una cuenta (alta o edición). Normaliza los campos antes de persistir y
 * programa el recordatorio de vencimiento (un día antes).
 */
class SaveAccountUseCase(
    private val repository: AccountRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(account: Account): Long {
        val normalized = account.copy(
            email = account.email.trim(),
            password = account.password.trim(),
            profileName = account.profileName.trim(),
            pin = account.pin.trim(),
            platform = account.platform.trim(),
            clientPhone = account.clientPhone.trim(),
            durationDays = account.durationDays.coerceAtLeast(0)
        )
        val id = repository.saveAccount(normalized)
        reminderScheduler.schedule(normalized.copy(id = id))
        return id
    }
}
