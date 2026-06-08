package com.dloren.mispantallas.domain.util

/** Abstracción de la versión instalada, para no depender de BuildConfig en el dominio. */
interface VersionProvider {
    val versionCode: Int
}
