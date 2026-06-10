package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.repository.AccountRepository

/** Provee todas las cuentas para generar un respaldo. */
class ExportAccountsUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke() = repository.getAllOnce()
}
