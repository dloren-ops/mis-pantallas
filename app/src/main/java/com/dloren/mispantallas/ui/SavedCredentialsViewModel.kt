package com.dloren.mispantallas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.AppDatabase
import com.dloren.mispantallas.data.SavedCredential
import com.dloren.mispantallas.data.SavedCredentialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedCredentialsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: SavedCredentialRepository =
        SavedCredentialRepository(AppDatabase.getInstance(app).savedCredentialDao())

    val credentials: StateFlow<List<SavedCredential>> = repository.credentials
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun save(email: String, password: String, note: String) {
        viewModelScope.launch {
            repository.save(SavedCredential(email = email, password = password, note = note))
        }
    }

    fun delete(credential: SavedCredential) {
        viewModelScope.launch { repository.delete(credential) }
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
                return SavedCredentialsViewModel(app) as T
            }
        }
    }
}
