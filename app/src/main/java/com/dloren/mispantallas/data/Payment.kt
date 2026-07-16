package com.dloren.mispantallas.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Un pago registrado para una cuenta alquilada, para llevar el historial de cobros. */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)
