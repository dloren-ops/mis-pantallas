package com.dloren.mispantallas.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Fechas alineadas a medianoche en la zona horaria local del dispositivo,
 * en vez de bloques de 24 horas exactas contadas desde un timestamp arbitrario.
 */
object DateUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(zone)

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun LocalDate.toStartOfDayMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()

    fun nowStartOfDayMillis(): Long = today().toStartOfDayMillis()

    /** El DatePicker de Compose codifica la fecha elegida como medianoche en UTC. */
    fun fromPickerMillis(utcMillis: Long): Long =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().toStartOfDayMillis()

    /** Inverso de [fromPickerMillis]: convierte medianoche local a lo que espera el picker. */
    fun toPickerMillis(localStartOfDayMillis: Long): Long =
        toLocalDate(localStartOfDayMillis).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
