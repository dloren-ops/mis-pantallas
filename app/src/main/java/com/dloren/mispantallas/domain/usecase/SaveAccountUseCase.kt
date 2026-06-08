package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.repository.AccountRepository

/**
 * Guarda una cuenta (alta o edición). Normaliza los campos de texto antes de
 * persistir y asegura una duración no negativa.
 */
class SaveAccountUseCase(private val repository: AccountRepository) {
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
        return repository.saveAccount(normalized)
    }
}
