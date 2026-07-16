package com.dloren.mispantallas.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.AppUpdateInfo
import com.dloren.mispantallas.data.AppUpdateRepository
import com.dloren.mispantallas.util.ApkInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la descarga/instalación de una actualización fuera de Play Store. */
sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data object Downloading : UpdateDownloadState()
    data object NeedsInstallPermission : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

/** Resultado de una búsqueda de actualización disparada a mano (botón en Ajustes). */
sealed class ManualCheckResult {
    data object Checking : ManualCheckResult()
    data object UpToDate : ManualCheckResult()
    data object Error : ManualCheckResult()
}

class AppUpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppUpdateRepository()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _manualCheckResult = MutableStateFlow<ManualCheckResult?>(null)
    val manualCheckResult: StateFlow<ManualCheckResult?> = _manualCheckResult.asStateFlow()

    /** Compara contra Firestore; si hay una versión más nueva la deja disponible para ofrecer. */
    fun checkForUpdate() {
        viewModelScope.launch { fetchAndMaybeOfferUpdate() }
    }

    /** Igual que [checkForUpdate] pero disparado a mano (botón en Ajustes): informa el
     *  resultado aunque no haya actualización, para que el usuario sepa que sí se buscó. */
    fun checkForUpdateNow() {
        viewModelScope.launch {
            _manualCheckResult.value = ManualCheckResult.Checking
            val foundUpdate = fetchAndMaybeOfferUpdate()
            _manualCheckResult.value = when (foundUpdate) {
                null -> ManualCheckResult.Error
                true -> null
                false -> ManualCheckResult.UpToDate
            }
        }
    }

    fun consumeManualCheckResult() {
        _manualCheckResult.value = null
    }

    /** null = error de red/Firestore, true = había una versión nueva (ya ofrecida), false = al día. */
    private suspend fun fetchAndMaybeOfferUpdate(): Boolean? {
        val info = runCatching { repository.fetchLatest() }.getOrNull() ?: return null
        val isNewer = info.versionCode > currentVersionCode() && info.apkUrl.isNotBlank()
        if (isNewer) _availableUpdate.value = info
        return isNewer
    }

    fun downloadAndInstall() {
        val info = _availableUpdate.value ?: return
        val context = getApplication<Application>()
        if (!ApkInstaller.canInstallApks(context)) {
            _downloadState.value = UpdateDownloadState.NeedsInstallPermission
            return
        }
        viewModelScope.launch {
            _downloadState.value = UpdateDownloadState.Downloading
            runCatching { ApkInstaller.downloadApk(context, info.apkUrl) }
                .onSuccess { file ->
                    _downloadState.value = UpdateDownloadState.Idle
                    ApkInstaller.installApk(context, file)
                }
                .onFailure { error ->
                    _downloadState.value = UpdateDownloadState.Error(error.message ?: "download_failed")
                }
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    private fun currentVersionCode(): Long {
        val context = getApplication<Application>()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as Application
                return AppUpdateViewModel(app) as T
            }
        }
    }
}
