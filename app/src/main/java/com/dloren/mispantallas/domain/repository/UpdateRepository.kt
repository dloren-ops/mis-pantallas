package com.dloren.mispantallas.domain.repository

import com.dloren.mispantallas.domain.model.AppRelease

/** Contrato para obtener información de la última versión publicada. */
interface UpdateRepository {
    /** Devuelve el último release publicado, o null si no hay ninguno. */
    suspend fun getLatestRelease(): AppRelease?
}
