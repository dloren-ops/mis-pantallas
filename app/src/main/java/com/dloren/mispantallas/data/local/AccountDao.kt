package com.dloren.mispantallas.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query(
        "SELECT * FROM accounts ORDER BY " +
            "CASE WHEN status = 'SOLD' THEN 1 ELSE 0 END ASC, " +
            "(soldDateMillis + durationDays * 86400000) ASC"
    )
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Delete
    suspend fun delete(entity: AccountEntity)
}
