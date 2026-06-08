package com.dloren.mispantallas.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.installer.ApkInstaller
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AppRelease
import com.dloren.mispantallas.domain.model.UpdateResult
import com.dloren.mispantallas.domain.usecase.CheckForUpdateUseCase
import com.dloren.mispantallas.domain.usecase.MarkAsSoldUseCase
import com.dloren.mispantallas.domain.usecase.ObserveAccountsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado del flujo de actualización para la pantalla de lista. */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val release: AppRelease, val downloading: Boolean = false) : UpdateUiState
}

/** Eventos puntuales (one-shot) que la pantalla traduce a Toast. */
sealed interface UpdateEvent {
    data object UpToDate : UpdateEvent
    data object NoReleases : UpdateEvent
    data object PermissionRequested : UpdateEvent
    data class Error(val message: String) : UpdateEvent
}

class AccountListViewModel(
    observeAccounts: ObserveAccountsUseCase,
    private val checkForUpdate: CheckForUpdateUseCase,
    private val markAsSold: MarkAsSoldUseCase,
    private val apkInstaller: ApkInstaller
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = observeAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _events = Channel<UpdateEvent>(Channel.BUFFERED)
    val events: Flow<UpdateEvent> = _events.receiveAsFlow()

    fun checkForUpdates() {
        if (_updateState.value is UpdateUiState.Checking) return
        _updateState.value = UpdateUiState.Checking
        viewModelScope.launch {
            when (val result = checkForUpdate()) {
                is UpdateResult.Available -> _updateState.value =
                    UpdateUiState.Available(result.release)
                is UpdateResult.UpToDate -> {
                    _updateState.value = UpdateUiState.Idle
                    _events.send(UpdateEvent.UpToDate)
                }
                is UpdateResult.NoReleases -> {
                    _updateState.value = UpdateUiState.Idle
                    _events.send(UpdateEvent.NoReleases)
                }
                is UpdateResult.Error -> {
                    _updateState.value = UpdateUiState.Idle
                    _events.send(UpdateEvent.Error(result.message))
                }
            }
        }
    }

    fun downloadAndInstall() {
        val current = _updateState.value
        if (current !is UpdateUiState.Available || current.downloading) return
        _updateState.value = current.copy(downloading = true)
        viewModelScope.launch {
            try {
                val apk = apkInstaller.download(current.release)
                val launched = apkInstaller.install(apk)
                if (!launched) _events.send(UpdateEvent.PermissionRequested)
            } catch (e: Exception) {
                _events.send(UpdateEvent.Error(e.message ?: ""))
            } finally {
                _updateState.value = UpdateUiState.Idle
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateUiState.Idle
    }

    /** Al enviar por WhatsApp desde la app, si no estaba vendida la marca vendida. */
    fun markSoldIfNeeded(account: Account) {
        viewModelScope.launch { markAsSold(account) }
    }
}
