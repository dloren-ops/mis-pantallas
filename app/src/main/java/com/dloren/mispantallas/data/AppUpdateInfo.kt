package com.dloren.mispantallas.data

/** Metadatos de la última versión publicada, leídos desde Firestore. */
data class AppUpdateInfo(
    val versionCode: Long = 0L,
    val versionName: String = "",
    val apkUrl: String = "",
    val changelog: String = ""
)
