package com.dloren.mispantallas.data.repository

import com.dloren.mispantallas.data.local.AccountDao
import com.dloren.mispantallas.data.mapper.toDomain
import com.dloren.mispantallas.data.mapper.toEntity
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(private val dao: AccountDao) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAccount(id: Long): Account? = dao.getById(id)?.toDomain()

    override suspend fun getAllOnce(): List<Account> = dao.getAll().map { it.toDomain() }

    override suspend fun saveAccount(account: Account): Long {
        return if (account.id == 0L) {
            dao.insert(account.toEntity())
        } else {
            dao.update(account.toEntity())
            account.id
        }
    }

    override suspend fun deleteAccount(account: Account) = dao.delete(account.toEntity())
}
