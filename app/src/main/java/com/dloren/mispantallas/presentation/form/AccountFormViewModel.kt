package com.dloren.mispantallas.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.usecase.DeleteAccountUseCase
import com.dloren.mispantallas.domain.usecase.GetAccountUseCase
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
    consumeSharedDraft: () -> Account?
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<Long>("accountId") ?: 0L

    private val _uiState = MutableStateFlow(AccountFormUiState())
    val uiState: StateFlow<AccountFormUiState> = _uiState.asStateFlow()

    init {
        val isEditing = accountId > 0L
        if (isEditing) {
            viewModelScope.launch {
                getAccount(accountId)?.let { acc -> _uiState.value = acc.toFormState(isEditing = true) }
            }
        } else {
            // Si llegó un borrador por "Compartir", prellenar.
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
        startDateMillis = startDateMillis
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
        startDateMillis = startDateMillis
    )
}
