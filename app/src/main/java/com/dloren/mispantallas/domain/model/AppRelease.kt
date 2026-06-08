package com.dloren.mispantallas.domain.model

/**
 * Representa una versión publicada de la app (un "Release" de GitHub).
 *
 * @param versionCode código de versión derivado del tag (v1.0.<n> -> n).
 * @param tag etiqueta del release (ej. "v1.0.42").
 * @param notes notas/descripción del release.
 * @param apkUrl URL de descarga directa del APK.
 */
data class AppRelease(
    val versionCode: Int,
    val tag: String,
    val notes: String,
    val apkUrl: String
)
