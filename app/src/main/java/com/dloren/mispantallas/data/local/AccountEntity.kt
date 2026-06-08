package com.dloren.mispantallas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Modelo de persistencia (Room) de una cuenta. Vive solo en la capa de datos. */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String = "",
    val password: String = "",
    val profileName: String = "",
    val pin: String = "",
    val platform: String = "",
    val clientPhone: String = "",
    val durationDays: Int = 30,
    val startDateMillis: Long = 0L,
    /** "NOT_SOLD" o "SOLD". */
    val status: String = "NOT_SOLD",
    val soldDateMillis: Long = 0L,
    val renewEveryDays: Int = 0,
    val providerStartMillis: Long = 0L
)
