package com.dloren.mispantallas.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dloren.mispantallas.R
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.presentation.AppViewModelProvider
import com.dloren.mispantallas.presentation.whatsapp.WhatsAppLauncher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AccountFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }

    // Volver atrás cuando la operación terminó.
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    fun currentAccount() = Account(
        id = state.id,
        email = state.email,
        password = state.password,
        profileName = state.profileName,
        pin = state.pin,
        platform = state.platform,
        clientPhone = state.clientPhone,
        durationDays = state.durationText.toIntOrNull() ?: 30,
        startDateMillis = state.startDateMillis
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.edit_account else R.string.new_account
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pegado inteligente: pegar todo junto y autocompletar.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.paste_data),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.paste_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val ok = viewModel.applyPastedData(pasteText)
                            Toast.makeText(
                                context,
                                if (ok) R.string.paste_detected else R.string.paste_not_detected,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = pasteText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.detect_fill))
                    }
                }
            }

            OutlinedTextField(
                value = state.platform,
                onValueChange = viewModel::onPlatformChange,
                label = { Text(stringResource(R.string.field_platform)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.field_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.field_password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.profileName,
                onValueChange = viewModel::onProfileChange,
                label = { Text(stringResource(R.string.field_profile)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.pin,
                onValueChange = viewModel::onPinChange,
                label = { Text(stringResource(R.string.field_pin)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.clientPhone,
                onValueChange = viewModel::onClientPhoneChange,
                label = { Text(stringResource(R.string.field_client_phone)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.durationText,
                onValueChange = viewModel::onDurationChange,
                label = { Text(stringResource(R.string.field_duration)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dateFormat.format(Date(state.startDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_start_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }

            OutlinedButton(
                onClick = { WhatsAppLauncher.send(context, currentAccount()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text("  " + stringResource(R.string.send_whatsapp))
            }

            if (state.isEditing) {
                OutlinedButton(
                    onClick = viewModel::delete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("  " + stringResource(R.string.delete))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onStartDateChange(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
