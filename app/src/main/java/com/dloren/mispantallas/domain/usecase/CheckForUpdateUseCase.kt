package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.UpdateResult
import com.dloren.mispantallas.domain.repository.UpdateRepository
import com.dloren.mispantallas.domain.util.VersionProvider

/**
 * Comprueba si hay una versión más nueva publicada comparando el versionCode del
 * último release con el de la app instalada.
 */
class CheckForUpdateUseCase(
    private val updateRepository: UpdateRepository,
    private val versionProvider: VersionProvider
) {
    suspend operator fun invoke(): UpdateResult {
        return try {
            val release = updateRepository.getLatestRelease()
                ?: return UpdateResult.NoReleases
            if (release.versionCode > versionProvider.versionCode) {
                UpdateResult.Available(release)
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Error desconocido")
        }
    }
}
