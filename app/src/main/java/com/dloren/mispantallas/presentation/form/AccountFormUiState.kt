package com.dloren.mispantallas.presentation.form

import com.dloren.mispantallas.domain.model.AccountStatus

/** Estado de la pantalla de formulario (alta/edición). */
data class AccountFormUiState(
    val id: Long = 0L,
    val isEditing: Boolean = false,
    val email: String = "",
    val password: String = "",
    val profileName: String = "",
    val pin: String = "",
    val platform: String = "",
    val clientPhone: String = "",
    val durationText: String = "30",
    val startDateMillis: Long = System.currentTimeMillis(),
    val status: AccountStatus = AccountStatus.NOT_SOLD,
    val soldDateMillis: Long = 0L,
    /** Renovación propia con el proveedor. */
    val selfRenewEnabled: Boolean = false,
    val renewEveryText: String = "15",
    val providerStartMillis: Long = System.currentTimeMillis(),
    /** true cuando la operación terminó y hay que volver atrás. */
    val finished: Boolean = false
)
