package com.dloren.mispantallas.domain.repository

import com.dloren.mispantallas.domain.model.Account
import kotlinx.coroutines.flow.Flow

/** Contrato de acceso a las cuentas. La implementación vive en la capa de datos. */
interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun getAccount(id: Long): Account?
    suspend fun saveAccount(account: Account): Long
    suspend fun deleteAccount(account: Account)
}
