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
     * Genera cuentas con nombres de perfil y PIN predefinidos, en orden:
     * perfiles [PROFILE_NAMES] y PIN 9876, 8876, 7876, ... (primer dígito que baja).
     */
    fun generateNamedProfiles(
        email: String,
        password: String,
        platform: String,
        durationDays: Int,
        count: Int
    ) {
        val n = count.coerceIn(1, PROFILE_NAMES.size)
        viewModelScope.launch {
            for (i in 0 until n) {
                saveAccount(
                    Account(
                        email = email.trim(),
                        password = password.trim(),
                        platform = platform.trim(),
                        profileName = PROFILE_NAMES[i],
                        pin = "${9 - i}876",
                        durationDays = durationDays.coerceAtLeast(1),
                        status = AccountStatus.NOT_SOLD
                    )
                )
            }
        }
    }

    companion object {
        /** Nombres de perfil predefinidos, en orden de asignación. */
        val PROFILE_NAMES = listOf("Mura", "Bura", "Dayira", "Marara", "Anya", "Maun", "Iyira")
    }
}
