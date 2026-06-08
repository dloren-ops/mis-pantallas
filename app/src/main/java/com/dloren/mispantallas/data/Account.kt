package com.dloren.mispantallas.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

/**
 * Representa una cuenta / pantalla de streaming en alquiler.
 *
 * @param startDateMillis fecha de inicio del alquiler (epoch millis).
 * @param durationDays duración del alquiler en días.
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String = "",
    val password: String = "",
    val profileName: String = "",
    val pin: String = "",
    val platform: String = "",
    val clientPhone: String = "",
    val durationDays: Int = 30,
    val startDateMillis: Long = System.currentTimeMillis()
) {
    /** Fecha en la que vence el alquiler (epoch millis). */
    val endDateMillis: Long
        get() = startDateMillis + TimeUnit.DAYS.toMillis(durationDays.toLong())

    /**
     * Días restantes hasta el vencimiento (puede ser negativo si ya venció).
     * Se calcula respecto al momento [now].
     */
    fun remainingDays(now: Long = System.currentTimeMillis()): Long {
        val diff = endDateMillis - now
        // Redondeo hacia arriba: mientras quede algo del día, cuenta como 1.
        return Math.ceil(diff.toDouble() / TimeUnit.DAYS.toMillis(1)).toLong()
    }

    /** true si el alquiler ya venció. */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now >= endDateMillis
}
