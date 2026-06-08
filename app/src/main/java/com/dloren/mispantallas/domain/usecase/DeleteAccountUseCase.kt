package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.repository.AccountRepository

/** Elimina una cuenta. */
class DeleteAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(account: Account) = repository.deleteAccount(account)
}
