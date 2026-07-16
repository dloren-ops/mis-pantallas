package com.dloren.mispantallas.data

import kotlinx.coroutines.flow.Flow

class PaymentRepository(private val dao: PaymentDao) {

    fun observeForAccount(accountId: Long): Flow<List<Payment>> = dao.observeForAccount(accountId)

    suspend fun save(payment: Payment): Long = dao.insert(payment)

    suspend fun delete(payment: Payment) = dao.delete(payment)
}
