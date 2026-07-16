package com.dloren.mispantallas.util

import java.util.Locale

/** Formatea un monto con separador de miles y 2 decimales, según el locale del dispositivo. */
fun formatCurrency(amount: Double): String = String.format(Locale.getDefault(), "%,.2f", amount)
