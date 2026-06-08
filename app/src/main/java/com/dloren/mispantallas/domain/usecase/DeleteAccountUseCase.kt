package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.reminder.ReminderScheduler
import com.dloren.mispantallas.domain.repository.AccountRepository

/** Elimina una cuenta y cancela su recordatorio de vencimiento. */
class DeleteAccountUseCase(
    private val repository: AccountRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(account: Account) {
        repository.deleteAccount(account)
        reminderScheduler.cancel(account.id)
    }
}
