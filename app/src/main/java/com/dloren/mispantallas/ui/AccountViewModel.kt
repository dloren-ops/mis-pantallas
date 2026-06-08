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

    fun delete(account: Account) {
        viewModelScope.launch { repository.delete(account) }
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
