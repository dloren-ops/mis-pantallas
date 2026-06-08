package com.dloren.mispantallas.domain.model

import java.util.concurrent.TimeUnit

/**
 * Modelo de dominio de una cuenta / pantalla de streaming.
 *
 * Maneja dos líneas de tiempo independientes:
 *  - **Venta al cliente**: cuando [status] es [AccountStatus.SOLD], el conteo
 *    corre [durationDays] (por defecto 30) desde [soldDateMillis].
 *  - **Renovación propia con el proveedor**: si [renewEveryDays] > 0, sirve para
 *    recordarte renovar la cuenta que vos comprás (ej. cada 15 días) desde
 *    [providerStartMillis].
 *
 * Es un modelo puro (sin Android ni Room).
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
    /** Fecha de alta (referencia histórica). */
    val startDateMillis: Long = System.currentTimeMillis(),
    val status: AccountStatus = AccountStatus.NOT_SOLD,
    /** Momento en que se marcó como vendida (ancla del conteo del cliente). */
    val soldDateMillis: Long = 0L,
    /** Ciclo de renovación propia con el proveedor en días (0 = desactivado). */
    val renewEveryDays: Int = 0,
    /** Última vez que compraste/renovaste con el proveedor (ancla del recordatorio propio). */
    val providerStartMillis: Long = System.currentTimeMillis()
) {
    private val dayMillis: Long get() = TimeUnit.DAYS.toMillis(1)

    val isSold: Boolean get() = status == AccountStatus.SOLD

    // ----- Venta al cliente -----

    /** Fecha de vencimiento para el cliente (válida solo si está vendida). */
    val clientEndDateMillis: Long
        get() = soldDateMillis + TimeUnit.DAYS.toMillis(durationDays.toLong())

    /** Días restantes para el cliente, contados por **día de calendario**. Solo si está vendida. */
    fun remainingClientDays(now: Long = System.currentTimeMillis()): Long =
        calendarDaysBetween(now, clientEndDateMillis)

    fun isClientExpired(now: Long = System.currentTimeMillis()): Boolean =
        isSold && now >= clientEndDateMillis

    // ----- Renovación propia (proveedor) -----

    val hasProviderRenewal: Boolean get() = renewEveryDays > 0

    /** Próxima fecha en que debés renovar con el proveedor. */
    val providerRenewMillis: Long
        get() = providerStartMillis + TimeUnit.DAYS.toMillis(renewEveryDays.toLong())

    fun remainingProviderDays(now: Long = System.currentTimeMillis()): Long =
        calendarDaysBetween(now, providerRenewMillis)

    private companion object {
        /**
         * Diferencia en días de calendario entre [from] y [to] (de medianoche a
         * medianoche, zona horaria local). Ej.: vendida ayer con 30 días -> hoy 29;
         * vendida hoy -> 30.
         */
        fun calendarDaysBetween(from: Long, to: Long): Long {
            val dayMs = TimeUnit.DAYS.toMillis(1)
            return Math.round((startOfDay(to) - startOfDay(from)).toDouble() / dayMs)
        }

        fun startOfDay(millis: Long): Long {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
