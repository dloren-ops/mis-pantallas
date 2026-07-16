package com.dloren.mispantallas.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.ClientLicense
import com.dloren.mispantallas.data.LicensePreferences
import com.dloren.mispantallas.data.LicenseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Estado de licenciamiento de esta instalación de la app. */
sealed class LicenseState {
    data object Loading : LicenseState()
    data object NeedsCode : LicenseState()
    data object Admin : LicenseState()
    data class Client(val expiresAtMillis: Long, val isReadOnly: Boolean) : LicenseState()
}

class LicenseViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = LicenseRepository()
    private val prefs = LicensePreferences(app)
    private val auth = FirebaseAuth.getInstance()

    private val _licenseState = MutableStateFlow<LicenseState>(LicenseState.Loading)
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    private val _codeError = MutableStateFlow(false)
    val codeError: StateFlow<Boolean> = _codeError.asStateFlow()

    private val _codeInUse = MutableStateFlow(false)
    val codeInUse: StateFlow<Boolean> = _codeInUse.asStateFlow()

    private val deviceId: String by lazy {
        Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    private val _adminLoginError = MutableStateFlow(false)
    val adminLoginError: StateFlow<Boolean> = _adminLoginError.asStateFlow()

    private val _clients = MutableStateFlow<List<ClientLicense>>(emptyList())
    val clients: StateFlow<List<ClientLicense>> = _clients.asStateFlow()

    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError.asStateFlow()

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        if (auth.currentUser != null) {
            _licenseState.value = LicenseState.Admin
            refreshClients()
            return
        }
        val code = prefs.readCachedCode()
        if (code == null) {
            _licenseState.value = LicenseState.NeedsCode
            return
        }
        emitClientState(prefs.readCachedExpiresAt() ?: 0L)
        refreshFromRemote(code)
    }

    /** Vuelve a validar contra Firestore, por ejemplo al reanudar la app. */
    fun revalidate() {
        if (_licenseState.value !is LicenseState.Client) return
        viewModelScope.launch {
            prefs.readCachedCode()?.let { refreshFromRemote(it) }
        }
    }

    private suspend fun refreshFromRemote(code: String) {
        val license = runCatching { repository.fetchByCode(code) }.getOrNull() ?: return
        prefs.saveLicense(code, license.expiresAtMillis)
        emitClientState(license.expiresAtMillis)
    }

    private fun emitClientState(expiresAtMillis: Long) {
        _licenseState.value = LicenseState.Client(
            expiresAtMillis = expiresAtMillis,
            isReadOnly = System.currentTimeMillis() > expiresAtMillis
        )
    }

    /** Valida un código de cliente ingresado en [LicenseGateScreen]. */
    fun submitCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _codeError.value = false
            _codeInUse.value = false
            val license = runCatching { repository.fetchByCode(trimmed) }.getOrNull()
            when {
                license == null -> _codeError.value = true
                license.boundDeviceId.isNotBlank() && license.boundDeviceId != deviceId -> {
                    _codeInUse.value = true
                }
                else -> {
                    if (license.boundDeviceId.isBlank()) {
                        runCatching { repository.bindDevice(trimmed, deviceId) }
                    }
                    prefs.saveLicense(trimmed, license.expiresAtMillis)
                    emitClientState(license.expiresAtMillis)
                }
            }
        }
    }

    /** Inicia sesión como administrador desde [LicenseGateScreen]. */
    fun loginAdmin(email: String, password: String) {
        viewModelScope.launch {
            _adminLoginError.value = false
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _licenseState.value = LicenseState.Admin
                refreshClients()
            } catch (e: Exception) {
                _adminLoginError.value = true
            }
        }
    }

    fun logoutAdmin() {
        auth.signOut()
        _licenseState.value = LicenseState.NeedsCode
    }

    fun refreshClients() {
        viewModelScope.launch {
            _clients.value = runCatching { repository.listAll() }.getOrDefault(emptyList())
        }
    }

    fun createClient(name: String, phone: String, onCreated: (ClientLicense) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.createClient(name, phone) }
                .onSuccess { license ->
                    refreshClients()
                    onCreated(license)
                }
                .onFailure { e -> _adminError.value = e.message }
        }
    }

    fun extendClient(code: String, months: Int) {
        viewModelScope.launch {
            runCatching { repository.activate(code, months) }
                .onSuccess { refreshClients() }
                .onFailure { e -> _adminError.value = e.message }
        }
    }

    fun clearAdminError() {
        _adminError.value = null
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
                return LicenseViewModel(app) as T
            }
        }
    }
}
