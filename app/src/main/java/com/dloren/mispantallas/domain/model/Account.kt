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

    /** Días restantes para el cliente (redondeo hacia arriba). Solo si está vendida. */
    fun remainingClientDays(now: Long = System.currentTimeMillis()): Long {
        val diff = clientEndDateMillis - now
        return Math.ceil(diff.toDouble() / dayMillis).toLong()
    }

    fun isClientExpired(now: Long = System.currentTimeMillis()): Boolean =
        isSold && now >= clientEndDateMillis

    // ----- Renovación propia (proveedor) -----

    val hasProviderRenewal: Boolean get() = renewEveryDays > 0

    /** Próxima fecha en que debés renovar con el proveedor. */
    val providerRenewMillis: Long
        get() = providerStartMillis + TimeUnit.DAYS.toMillis(renewEveryDays.toLong())

    fun remainingProviderDays(now: Long = System.currentTimeMillis()): Long {
        val diff = providerRenewMillis - now
        return Math.ceil(diff.toDouble() / dayMillis).toLong()
    }
}
