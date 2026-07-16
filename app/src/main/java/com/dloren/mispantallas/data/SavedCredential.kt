package com.dloren.mispantallas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Correo y contraseña guardados para reutilizar al crear una cuenta nueva más adelante. */
@Entity(tableName = "saved_credentials")
data class SavedCredential(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String = "",
    val password: String = "",
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
