package com.dloren.mispantallas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCredentialDao {

    @Query("SELECT * FROM saved_credentials ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<SavedCredential>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credential: SavedCredential): Long

    @Delete
    suspend fun delete(credential: SavedCredential)
}
