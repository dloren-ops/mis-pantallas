package com.dloren.mispantallas.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Consulta la última versión publicada en Firestore (documento cargado a mano desde
 * la consola de Firebase), para ofrecer actualizar la app fuera de Play Store.
 */
class AppUpdateRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun fetchLatest(): AppUpdateInfo? {
        val snapshot = firestore.collection("app_config").document("update").get().await()
        if (!snapshot.exists()) return null
        return AppUpdateInfo(
            versionCode = snapshot.getLong("versionCode") ?: 0L,
            versionName = snapshot.getString("versionName") ?: "",
            apkUrl = snapshot.getString("apkUrl") ?: "",
            changelog = snapshot.getString("changelog") ?: ""
        )
    }
}
