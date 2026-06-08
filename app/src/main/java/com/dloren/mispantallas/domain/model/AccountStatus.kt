package com.dloren.mispantallas.domain.model

/** Estado de venta de una cuenta. */
enum class AccountStatus {
    /** Aún no se vendió: no corre el conteo para el cliente. */
    NOT_SOLD,

    /** Vendida: corre el conteo de [Account.durationDays] desde la fecha de venta. */
    SOLD
}
