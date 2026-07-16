package com.dloren.mispantallas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PausedAccountDao {

    @Query("SELECT * FROM paused_accounts ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<PausedAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: PausedAccount): Long

    @Delete
    suspend fun delete(account: PausedAccount)
}
