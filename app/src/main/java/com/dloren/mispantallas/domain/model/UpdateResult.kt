package com.dloren.mispantallas.domain.model

/** Resultado de comprobar si hay una actualización disponible. */
sealed interface UpdateResult {
    data class Available(val release: AppRelease) : UpdateResult
    data object UpToDate : UpdateResult
    data object NoReleases : UpdateResult
    data class Error(val message: String) : UpdateResult
}
