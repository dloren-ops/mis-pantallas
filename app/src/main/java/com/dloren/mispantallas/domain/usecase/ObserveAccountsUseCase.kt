package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

/** Emite la lista de cuentas observando los cambios en el repositorio. */
class ObserveAccountsUseCase(private val repository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> = repository.observeAccounts()
}
