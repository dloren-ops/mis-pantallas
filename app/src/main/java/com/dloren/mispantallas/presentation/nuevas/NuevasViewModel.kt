package com.dloren.mispantallas.presentation.nuevas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AccountStatus
import com.dloren.mispantallas.domain.usecase.MarkAsSoldUseCase
import com.dloren.mispantallas.domain.usecase.ObserveAccountsUseCase
import com.dloren.mispantallas.domain.usecase.SaveAccountUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Pantalla "Nuevas": cuentas aún **sin vender**. Al enviarlas por WhatsApp pasan a
 * vendidas (y se mueven a la lista principal). Incluye un generador para crear
 * varias cuentas con el mismo correo (una por cliente/pantalla).
 */
class NuevasViewModel(
    observeAccounts: ObserveAccountsUseCase,
    private val markAsSold: MarkAsSoldUseCase,
    private val saveAccount: SaveAccountUseCase
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = observeAccounts()
        .map { list ->
            list.filter { it.status == AccountStatus.NOT_SOLD }
                .sortedByDescending { it.startDateMillis }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Al enviar por WhatsApp: marca vendida (se mueve a la lista principal). */
    fun markSold(account: Account) {
        viewModelScope.launch { markAsSold(account) }
    }

    /**
     * Genera [count] cuentas sin vender con el mismo correo/contraseña/plataforma,
     * una por cliente (perfiles "Perfil 1".."Perfil N"). Listas para asignar y enviar.
     */
    fun generate(
        email: String,
        password: String,
        platform: String,
        durationDays: Int,
        count: Int
    ) {
        val n = count.coerceIn(1, 20)
        viewModelScope.launch {
            for (i in 1..n) {
                saveAccount(
                    Account(
                        email = email.trim(),
                        password = password.trim(),
                        platform = platform.trim(),
                        profileName = "Perfil $i",
                        durationDays = durationDays.coerceAtLeast(1),
                        status = AccountStatus.NOT_SOLD
                    )
                )
            }
        }
    }
}
