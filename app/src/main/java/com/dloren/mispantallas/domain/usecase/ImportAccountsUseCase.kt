package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.repository.AccountRepository

/** Inserta las cuentas de un respaldo como nuevas. Devuelve cuántas importó. */
class ImportAccountsUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(accounts: List<Account>): Int {
        accounts.forEach { repository.saveAccount(it.copy(id = 0L)) }
        return accounts.size
    }
}
