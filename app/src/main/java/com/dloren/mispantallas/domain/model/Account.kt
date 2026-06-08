package com.dloren.mispantallas.domain.model

import java.util.concurrent.TimeUnit

/**
 * Modelo de dominio de una cuenta / pantalla de streaming en alquiler.
 *
 * Es un modelo puro (sin dependencias de Android ni de Room): contiene los datos
 * y la lógica de negocio del alquiler (cuenta regresiva).
 */
data class Account(
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
     * Días restantes hasta el vencimiento (negativo si ya venció), respecto a [now].
     * Se redondea hacia arriba: mientras quede parte del día, cuenta como 1.
     */
    fun remainingDays(now: Long = System.currentTimeMillis()): Long {
        val diff = endDateMillis - now
        return Math.ceil(diff.toDouble() / TimeUnit.DAYS.toMillis(1)).toLong()
    }

    /** true si el alquiler ya venció. */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now >= endDateMillis
}
