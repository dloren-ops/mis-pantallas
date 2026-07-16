package com.dloren.mispantallas.data

import kotlinx.coroutines.flow.Flow

class PausedAccountRepository(private val dao: PausedAccountDao) {

    val accounts: Flow<List<PausedAccount>> = dao.observeAll()

    suspend fun save(account: PausedAccount): Long = dao.insert(account)

    suspend fun delete(account: PausedAccount) = dao.delete(account)
}
