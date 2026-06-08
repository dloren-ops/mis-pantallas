package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.repository.AccountRepository

/** Obtiene una cuenta por id (o null si no existe). */
class GetAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(id: Long): Account? = repository.getAccount(id)
}
