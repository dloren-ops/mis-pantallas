package com.dloren.mispantallas.util

/**
 * Duraciones en meses para renovar la licencia de la app (panel admin), no para el
 * alquiler de pantallas — eso se vende por días, ver [DAY_PRESETS].
 */
val MONTH_PRESETS = listOf(
    1 to "1 mes",
    2 to "2 meses",
    3 to "3 meses",
    6 to "6 meses",
    12 to "1 año"
)

const val DAYS_PER_MONTH = 30

/** Duraciones en días ofrecidas como acceso rápido para el alquiler de pantallas. */
val DAY_PRESETS = listOf(15, 30, 45, 60, 90, 180, 365)
