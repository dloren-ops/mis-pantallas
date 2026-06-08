package com.dloren.mispantallas.data

import kotlinx.coroutines.flow.Flow

class AccountRepository(private val dao: AccountDao) {

    val accounts: Flow<List<Account>> = dao.observeAll()

    suspend fun getById(id: Long): Account? = dao.getById(id)

    suspend fun save(account: Account): Long {
        return if (account.id == 0L) {
            dao.insert(account)
        } else {
            dao.update(account)
            account.id
        }
    }

    suspend fun delete(account: Account) = dao.delete(account)
}
