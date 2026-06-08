package com.dloren.mispantallas.presentation.renewals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.usecase.ObserveAccountsUseCase
import com.dloren.mispantallas.domain.usecase.SaveAccountUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Muestra solo las cuentas con **renovación propia** activada (las que vos comprás
 * y debés renovar), ordenadas por la que vence antes.
 */
class RenewalsViewModel(
    observeAccounts: ObserveAccountsUseCase,
    private val saveAccount: SaveAccountUseCase
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = observeAccounts()
        .map { list ->
            list.filter { it.hasProviderRenewal }
                .sortedBy { it.remainingProviderDays() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Reinicia el ciclo: la renovaste hoy, así que el próximo aviso parte de ahora. */
    fun renewedToday(account: Account) {
        viewModelScope.launch {
            saveAccount(account.copy(providerStartMillis = System.currentTimeMillis()))
        }
    }
}
