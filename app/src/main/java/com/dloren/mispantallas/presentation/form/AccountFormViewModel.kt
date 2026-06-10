package com.dloren.mispantallas.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AccountStatus
import com.dloren.mispantallas.domain.usecase.DeleteAccountUseCase
import com.dloren.mispantallas.domain.usecase.GetAccountUseCase
import com.dloren.mispantallas.domain.usecase.ParseSharedAccountUseCase
import com.dloren.mispantallas.domain.usecase.SaveAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val getAccount: GetAccountUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val parseSharedAccount: ParseSharedAccountUseCase,
    consumeSharedDraft: () -> Account?
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<Long>("accountId") ?: 0L

    private val _uiState = MutableStateFlow(AccountFormUiState())
    val uiState: StateFlow<AccountFormUiState> = _uiState.asStateFlow()

    init {
        val isEditing = accountId > 0L
        if (isEditing) {
            viewModelScope.launch {
                getAccount(accountId)?.let { acc ->
                    _uiState.value = acc.toFormState(isEditing = true)
                }
            }
        } else {
            consumeSharedDraft()?.let { draft ->
                _uiState.value = draft.toFormState(isEditing = false)
            }
        }
    }

    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v) }
    fun onProfileChange(v: String) = _uiState.update { it.copy(profileName = v) }
    fun onPinChange(v: String) = _uiState.update { it.copy(pin = v) }
    fun onPlatformChange(v: String) = _uiState.update { it.copy(platform = v) }
    fun onClientPhoneChange(v: String) = _uiState.update { it.copy(clientPhone = v) }
    fun onDurationChange(v: String) =
        _uiState.update { it.copy(durationText = v.filter { c -> c.isDigit() }) }

    fun onStartDateChange(millis: Long) = _uiState.update { it.copy(startDateMillis = millis) }

    fun onSelfRenewToggle(enabled: Boolean) = _uiState.update { it.copy(selfRenewEnabled = enabled) }
    fun onRenewEveryChange(v: String) =
        _uiState.update { it.copy(renewEveryText = v.filter { c -> c.isDigit() }) }

    /** Reinicia el ciclo de renovación propia (vos compraste/renovaste hoy). */
    fun onRenewedToday() =
        _uiState.update { it.copy(providerStartMillis = System.currentTimeMillis()) }

    /** Marca la cuenta como vendida desde el formulario (arranca el conteo al guardar). */
    fun markSoldNow() = _uiState.update {
        it.copy(status = AccountStatus.SOLD, soldDateMillis = System.currentTimeMillis())
    }

    /** Devuelve la cuenta a "no vendida" desde el formulario (se aplica al guardar). */
    fun markNotSoldNow() = _uiState.update {
        it.copy(status = AccountStatus.NOT_SOLD, soldDateMillis = 0L)
    }

    /** Renueva la venta: reinicia el conteo desde hoy (se aplica al guardar). */
    fun renewSaleNow() = _uiState.update {
        it.copy(status = AccountStatus.SOLD, soldDateMillis = System.currentTimeMillis())
    }

    /**
     * Aplica un texto pegado "a granel": detecta cada dato y completa los campos
     * correspondientes (sin pisar los que ya tengan valor si el texto no los trae).
     */
    fun applyPastedData(text: String): Boolean {
        val parsed = parseSharedAccount(text) ?: return false
        _uiState.update { s ->
            s.copy(
                email = parsed.email.ifBlank { s.email },
                password = parsed.password.ifBlank { s.password },
                profileName = parsed.profileName.ifBlank { s.profileName },
                pin = parsed.pin.ifBlank { s.pin },
                platform = parsed.platform.ifBlank { s.platform },
                clientPhone = parsed.clientPhone.ifBlank { s.clientPhone },
                durationText = if (parsed.durationDays != 30) {
                    parsed.durationDays.toString()
                } else {
                    s.durationText
                }
            )
        }
        return true
    }

    fun save() {
        viewModelScope.launch {
            saveAccount(_uiState.value.toAccount())
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        if (!_uiState.value.isEditing) return
        viewModelScope.launch {
            deleteAccount(_uiState.value.toAccount())
            _uiState.update { it.copy(finished = true) }
        }
    }

    private fun Account.toFormState(isEditing: Boolean) = AccountFormUiState(
        id = id,
        isEditing = isEditing,
        email = email,
        password = password,
        profileName = profileName,
        pin = pin,
        platform = platform,
        clientPhone = clientPhone,
        durationText = durationDays.toString(),
        startDateMillis = startDateMillis,
        status = status,
        soldDateMillis = soldDateMillis,
        selfRenewEnabled = renewEveryDays > 0,
        renewEveryText = if (renewEveryDays > 0) renewEveryDays.toString() else "15",
        providerStartMillis = providerStartMillis
    )

    private fun AccountFormUiState.toAccount() = Account(
        id = id,
        email = email,
        password = password,
        profileName = profileName,
        pin = pin,
        platform = platform,
        clientPhone = clientPhone,
        durationDays = durationText.toIntOrNull() ?: 30,
        startDateMillis = startDateMillis,
        status = status,
        soldDateMillis = soldDateMillis,
        renewEveryDays = if (selfRenewEnabled) (renewEveryText.toIntOrNull() ?: 0) else 0,
        providerStartMillis = providerStartMillis
    )
}
