package com.dloren.mispantallas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.AppDatabase
import com.dloren.mispantallas.data.PausedAccount
import com.dloren.mispantallas.data.PausedAccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PausedAccountsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: PausedAccountRepository =
        PausedAccountRepository(AppDatabase.getInstance(app).pausedAccountDao())

    val accounts: StateFlow<List<PausedAccount>> = repository.accounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun save(email: String, password: String, note: String) {
        viewModelScope.launch {
            repository.save(PausedAccount(email = email, password = password, note = note))
        }
    }

    fun delete(account: PausedAccount) {
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
                return PausedAccountsViewModel(app) as T
            }
        }
    }
}
