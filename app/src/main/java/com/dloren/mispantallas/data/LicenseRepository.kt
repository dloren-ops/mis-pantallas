package com.dloren.mispantallas.data

import com.dloren.mispantallas.util.DAYS_PER_MONTH
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class LicenseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val clients get() = firestore.collection("clients")

    /** Busca la licencia de un cliente por su código. Devuelve null si no existe. */
    suspend fun fetchByCode(code: String): ClientLicense? {
        val snapshot = clients.document(code).get().await()
        if (!snapshot.exists()) return null
        return snapshot.toObject(ClientLicense::class.java)?.copy(code = snapshot.id)
    }

    /** Lista todos los clientes. Requiere estar autenticado como administrador. */
    suspend fun listAll(): List<ClientLicense> {
        val snapshot = clients.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ClientLicense::class.java)?.copy(code = doc.id)
        }
    }

    /** Crea un nuevo cliente con un código único aún sin activar. */
    suspend fun createClient(name: String, phone: String): ClientLicense {
        val code = generateCode()
        val now = System.currentTimeMillis()
        val license = ClientLicense(
            code = code,
            name = name,
            phone = phone,
            createdAtMillis = now,
            activatedAtMillis = 0L,
            expiresAtMillis = 0L
        )
        clients.document(code).set(license).await()
        return license
    }

    /**
     * Ata el código a un dispositivo la primera vez que se activa, para que no pueda
     * reutilizarse en otras instalaciones. Falla (rechazada por las reglas de Firestore)
     * si el código ya está atado a otro dispositivo.
     */
    suspend fun bindDevice(code: String, deviceId: String) {
        clients.document(code).update("boundDeviceId", deviceId).await()
    }

    /**
     * Activa o renueva un cliente por [months] meses. Si el cliente sigue activo,
     * la extensión se suma a su vencimiento actual; si ya venció, cuenta desde ahora.
     */
    suspend fun activate(code: String, months: Int) {
        val now = System.currentTimeMillis()
        val current = fetchByCode(code)
        val base = maxOf(current?.expiresAtMillis ?: 0L, now)
        val newExpiresAt = base + TimeUnit.DAYS.toMillis(months * DAYS_PER_MONTH.toLong())
        clients.document(code).update(
            mapOf(
                "expiresAtMillis" to newExpiresAt,
                "activatedAtMillis" to now
            )
        ).await()
    }

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val raw = (1..8).map { alphabet.random() }.joinToString("")
        return "${raw.substring(0, 4)}-${raw.substring(4)}"
    }
}
