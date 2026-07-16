package com.dloren.mispantallas.data

/**
 * Licencia de una instalación de la app entregada a un revendedor/cliente.
 * Se persiste en Firestore bajo "clients/{code}" (el id del documento es [code]).
 */
data class ClientLicense(
    val code: String = "",
    val name: String = "",
    val phone: String = "",
    val createdAtMillis: Long = 0L,
    val activatedAtMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    val notes: String = "",
    val boundDeviceId: String = ""
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now > expiresAtMillis
}
