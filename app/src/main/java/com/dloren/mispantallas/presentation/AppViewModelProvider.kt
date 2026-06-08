package com.dloren.mispantallas.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dloren.mispantallas.MisPantallasApp
import com.dloren.mispantallas.presentation.form.AccountFormViewModel
import com.dloren.mispantallas.presentation.list.AccountListViewModel
import com.dloren.mispantallas.presentation.nuevas.NuevasViewModel
import com.dloren.mispantallas.presentation.renewals.RenewalsViewModel

/**
 * Fábrica central de ViewModels. Obtiene el contenedor de dependencias desde la
 * Application y cablea cada ViewModel con sus casos de uso.
 */
object AppViewModelProvider {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = misPantallasApp().container
            AccountListViewModel(
                observeAccounts = container.observeAccounts,
                checkForUpdate = container.checkForUpdate,
                markAsSold = container.markAsSold,
                apkInstaller = container.apkInstaller
            )
        }
        initializer {
            val container = misPantallasApp().container
            AccountFormViewModel(
                savedStateHandle = createSavedStateHandle(),
                getAccount = container.getAccount,
                saveAccount = container.saveAccount,
                deleteAccount = container.deleteAccount,
                parseSharedAccount = container.parseSharedAccount,
                consumeSharedDraft = container::consumeSharedDraft
            )
        }
        initializer {
            val container = misPantallasApp().container
            RenewalsViewModel(
                observeAccounts = container.observeAccounts,
                saveAccount = container.saveAccount
            )
        }
        initializer {
            val container = misPantallasApp().container
            NuevasViewModel(
                observeAccounts = container.observeAccounts,
                markAsSold = container.markAsSold,
                saveAccount = container.saveAccount
            )
        }
    }
}

/** Atajo para obtener la Application tipada desde los CreationExtras. */
private fun CreationExtras.misPantallasApp(): MisPantallasApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MisPantallasApp
