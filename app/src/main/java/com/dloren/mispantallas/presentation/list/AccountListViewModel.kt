package com.dloren.mispantallas.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.backup.BackupCodec
import com.dloren.mispantallas.data.installer.ApkInstaller
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AppRelease
import com.dloren.mispantallas.domain.model.UpdateResult
import com.dloren.mispantallas.domain.usecase.CheckForUpdateUseCase
import com.dloren.mispantallas.domain.usecase.ExportAccountsUseCase
import com.dloren.mispantallas.domain.usecase.ImportAccountsUseCase
import com.dloren.mispantallas.domain.usecase.MarkAsNotSoldUseCase
import com.dloren.mispantallas.domain.usecase.MarkAsSoldUseCase
import com.dloren.mispantallas.domain.usecase.ObserveAccountsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

/** Contadores para el resumen arriba de la lista. */
data class ListSummary(
    val sold: Int = 0,
    val dueSoon: Int = 0,
    val expired: Int = 0,
    val notSold: Int = 0
)

/** Filtro activo de la lista (se elige tocando los contadores). */
enum class ListFilter { SOLD, DUE_SOON, EXPIRED, NOT_SOLD }

class AccountListViewModel(
    observeAccounts: ObserveAccountsUseCase,
    private val checkForUpdate: CheckForUpdateUseCase,
    private val markAsSold: MarkAsSoldUseCase,
    private val markAsNotSold: MarkAsNotSoldUseCase,
    private val exportAccounts: ExportAccountsUseCase,
    private val importAccounts: ImportAccountsUseCase,
    private val apkInstaller: ApkInstaller
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(q: String) { _query.value = q }

    private val _filter = MutableStateFlow(ListFilter.SOLD)
    val filter: StateFlow<ListFilter> = _filter.asStateFlow()

    fun onFilterChange(f: ListFilter) { _filter.value = f }

    // Si hay búsqueda, busca en TODAS por correo/número/plataforma/perfil;
    // si no, aplica el filtro elegido en los contadores.
    val accounts: StateFlow<List<Account>> =
        combine(observeAccounts(), _query, _filter) { list, q, f ->
            val query = q.trim()
            if (query.isNotBlank()) {
                val digits = query.filter { it.isDigit() }
                list.filter { acc ->
                    acc.email.contains(query, ignoreCase = true) ||
                        acc.platform.contains(query, ignoreCase = true) ||
                        acc.profileName.contains(query, ignoreCase = true) ||
                        (digits.isNotEmpty() &&
                            acc.clientPhone.filter { c -> c.isDigit() }.contains(digits))
                }
            } else {
                when (f) {
                    ListFilter.SOLD -> list.filter { it.isSold }
                    ListFilter.DUE_SOON ->
                        list.filter { it.isSold && it.remainingClientDays() in 0..3 }
                    ListFilter.EXPIRED ->
                        list.filter { it.isSold && it.remainingClientDays() < 0 }
                    ListFilter.NOT_SOLD -> list.filter { !it.isSold }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    /** Resumen de contadores (sobre TODAS las cuentas). */
    val summary: StateFlow<ListSummary> = observeAccounts()
        .map { list ->
            ListSummary(
                sold = list.count { it.isSold },
                dueSoon = list.count { it.isSold && it.remainingClientDays() in 0..3 },
                expired = list.count { it.isSold && it.remainingClientDays() < 0 },
                notSold = list.count { !it.isSold }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListSummary()
        )

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

    /** Devuelve una cuenta vendida a "no vendida" (vuelve a Nuevas). */
    fun markNotSold(account: Account) {
        viewModelScope.launch { markAsNotSold(account) }
    }

    /** Genera el JSON de respaldo con todas las cuentas. */
    suspend fun buildBackupJson(): String = BackupCodec.toJson(exportAccounts())

    /** Restaura cuentas desde un JSON de respaldo. Devuelve cuántas importó. */
    suspend fun restoreBackup(json: String): Int =
        importAccounts(BackupCodec.fromJson(json))
}
