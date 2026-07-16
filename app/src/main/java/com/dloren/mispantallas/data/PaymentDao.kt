package com.dloren.mispantallas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE accountId = :accountId ORDER BY dateMillis DESC")
    fun observeForAccount(accountId: Long): Flow<List<Payment>>

    @Insert
    suspend fun insert(payment: Payment): Long

    @Delete
    suspend fun delete(payment: Payment)
}
