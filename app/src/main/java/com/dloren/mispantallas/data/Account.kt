package com.dloren.mispantallas.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dloren.mispantallas.util.DateUtils
import com.dloren.mispantallas.util.DateUtils.toStartOfDayMillis
import java.time.temporal.ChronoUnit

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
    val startDateMillis: Long = DateUtils.nowStartOfDayMillis(),
    /** "SOLD" (asignada a un cliente) o "NOT_SOLD" (todavía sin asignar). */
    val status: String = "SOLD",
    /** Momento en que se le asignó/vendió a un cliente (0 si nunca se asignó). */
    val soldDateMillis: Long = 0L,
    /** Si es mayor a 0, cada cuántos días hay que renovarla (independiente de [durationDays]). */
    val renewEveryDays: Int = 0,
    /** Momento en que arrancó la suscripción con el proveedor (puede diferir de [startDateMillis]). */
    val providerStartMillis: Long = 0L,
    /** Precio del alquiler por cada ciclo de [durationDays] (0 si no se cargó). */
    val price: Double = 0.0
) {
    /** Primer día (fecha local) en que el alquiler deja de ser válido. */
    private val endLocalDate
        get() = DateUtils.toLocalDate(startDateMillis).plusDays(durationDays.toLong())

    /** Momento (medianoche, zona horaria local) en que vence el alquiler. */
    val endDateMillis: Long
        get() = endLocalDate.toStartOfDayMillis()

    /**
     * Días de calendario restantes hasta el vencimiento (puede ser negativo si ya venció).
     * Se calcula por fecha local (medianoche a medianoche), no por bloques de 24 horas
     * desde el momento exacto en que se creó la cuenta.
     */
    fun remainingDays(now: Long = System.currentTimeMillis()): Long =
        ChronoUnit.DAYS.between(DateUtils.toLocalDate(now), endLocalDate)

    /** true si la fecha local de hoy ya alcanzó el día de vencimiento. */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = remainingDays(now) <= 0

    /** Primer día (fecha local) en que toca renovar con el proveedor, si aplica. */
    private val providerEndLocalDate
        get() = DateUtils.toLocalDate(if (providerStartMillis > 0) providerStartMillis else startDateMillis)
            .plusDays(renewEveryDays.toLong())

    /**
     * Días restantes hasta la próxima renovación con el proveedor (Netflix, Disney+, etc.),
     * independiente de [remainingDays]. null si [renewEveryDays] es 0 (no aplica).
     * Usa [providerStartMillis] como ancla, o [startDateMillis] si no se fijó uno propio.
     *
     * A diferencia de un ciclo automático, no se reinicia solo con el paso de los días:
     * una vez vencida queda vencida (el valor sigue bajando, cada vez más negativo) hasta
     * que se actualice manualmente la fecha de inicio con el proveedor (renovación real).
     */
    fun providerRemainingDays(now: Long = System.currentTimeMillis()): Long? {
        if (renewEveryDays <= 0) return null
        return ChronoUnit.DAYS.between(DateUtils.toLocalDate(now), providerEndLocalDate)
    }
}
