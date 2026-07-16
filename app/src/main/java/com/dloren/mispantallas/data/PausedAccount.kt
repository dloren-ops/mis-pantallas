package com.dloren.mispantallas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Correo y contraseña de una cuenta que se dejó en pausa (no en uso por ahora). */
@Entity(tableName = "paused_accounts")
data class PausedAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String = "",
    val password: String = "",
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
