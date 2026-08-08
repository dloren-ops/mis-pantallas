package com.dloren.mispantallas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.data.AccountRepository
import com.dloren.mispantallas.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: AccountRepository =
        AccountRepository(AppDatabase.getInstance(app).accountDao())

    val accounts: StateFlow<List<Account>> = repository.accounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    suspend fun getAccount(id: Long): Account? = repository.getById(id)

    /** Cuenta recibida por "Compartir" pendiente de prellenar en el formulario. */
    var sharedDraft: Account? = null
        private set

    fun setSharedDraft(account: Account?) {
        sharedDraft = account
    }

    fun consumeSharedDraft(): Account? {
        val draft = sharedDraft
        sharedDraft = null
        return draft
    }

    fun save(account: Account, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(account)
            onSaved(id)
        }
    }

    /**
     * Actualiza el correo de un grupo de cuentas (p. ej. otros perfiles del mismo login)
     * y las marca con aviso de cambio de correo pendiente, para que aparezcan en
     * "Avisos de correo" hasta que se les mande el WhatsApp con el dato nuevo.
     */
    fun updateEmailForAccounts(accounts: List<Account>, newEmail: String) {
        if (accounts.isEmpty()) return
        viewModelScope.launch {
            accounts.forEach {
                repository.save(it.copy(email = newEmail, emailChangeNoticePending = true))
            }
        }
    }

    /** Marca una pantalla como ya avisada del cambio de correo (se saca de "Avisos de correo"). */
    fun markEmailChangeNoticeSent(account: Account) {
        viewModelScope.launch {
            repository.save(account.copy(emailChangeNoticePending = false))
        }
    }

    fun delete(account: Account) {
        viewModelScope.launch { repository.delete(account) }
    }

    fun deleteAll(accounts: List<Account>) {
        if (accounts.isEmpty()) return
        viewModelScope.launch { repository.deleteAll(accounts) }
    }

    /** Inserta cuentas importadas desde un respaldo, siempre como filas nuevas. */
    fun importAccounts(imported: List<Account>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            imported.forEach { repository.save(it.copy(id = 0L)) }
            onDone()
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
                return AccountViewModel(app) as T
            }
        }
    }
}
