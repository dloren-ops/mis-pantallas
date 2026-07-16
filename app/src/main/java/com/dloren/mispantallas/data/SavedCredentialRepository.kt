package com.dloren.mispantallas.data

import kotlinx.coroutines.flow.Flow

class SavedCredentialRepository(private val dao: SavedCredentialDao) {

    val credentials: Flow<List<SavedCredential>> = dao.observeAll()

    suspend fun save(credential: SavedCredential): Long = dao.insert(credential)

    suspend fun delete(credential: SavedCredential) = dao.delete(credential)
}
