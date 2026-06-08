package com.dloren.mispantallas.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    accountId: Long,
    prefill: Account?,
    viewModel: AccountViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = accountId > 0L
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Estado del formulario.
    var loaded by rememberSaveable(accountId) { mutableStateOf(false) }
    var email by rememberSaveable(accountId) { mutableStateOf(prefill?.email ?: "") }
    var password by rememberSaveable(accountId) { mutableStateOf(prefill?.password ?: "") }
    var profile by rememberSaveable(accountId) { mutableStateOf(prefill?.profileName ?: "") }
    var pin by rememberSaveable(accountId) { mutableStateOf(prefill?.pin ?: "") }
    var platform by rememberSaveable(accountId) { mutableStateOf(prefill?.platform ?: "") }
    var clientPhone by rememberSaveable(accountId) { mutableStateOf(prefill?.clientPhone ?: "") }
    var duration by rememberSaveable(accountId) {
        mutableStateOf((prefill?.durationDays ?: 30).toString())
    }
    var startDate by rememberSaveable(accountId) {
        mutableStateOf(prefill?.startDateMillis ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Cargar la cuenta existente cuando se edita.
    LaunchedEffect(accountId) {
        if (isEditing && !loaded) {
            viewModel.getAccount(accountId)?.let { acc ->
                email = acc.email
                password = acc.password
                profile = acc.profileName
                pin = acc.pin
                platform = acc.platform
                clientPhone = acc.clientPhone
                duration = acc.durationDays.toString()
                startDate = acc.startDateMillis
            }
            loaded = true
        }
    }

    fun buildAccount(): Account = Account(
        id = if (isEditing) accountId else 0L,
        email = email.trim(),
        password = password.trim(),
        profileName = profile.trim(),
        pin = pin.trim(),
        platform = platform.trim(),
        clientPhone = clientPhone.trim(),
        durationDays = duration.toIntOrNull()?.coerceAtLeast(0) ?: 30,
        startDateMillis = startDate
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) R.string.edit_account else R.string.new_account
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
            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text(stringResource(R.string.field_platform)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.field_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.field_password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = profile,
                onValueChange = { profile = it },
                label = { Text(stringResource(R.string.field_profile)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text(stringResource(R.string.field_pin)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                label = { Text(stringResource(R.string.field_client_phone)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { new -> duration = new.filter { it.isDigit() } },
                label = { Text(stringResource(R.string.field_duration)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dateFormat.format(Date(startDate)),
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
                onClick = { viewModel.save(buildAccount()) { onDone() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }

            OutlinedButton(
                onClick = { WhatsAppHelper.sendToWhatsApp(context, buildAccount()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text(
                    text = "  " + stringResource(R.string.send_whatsapp)
                )
            }

            if (isEditing) {
                OutlinedButton(
                    onClick = {
                        viewModel.delete(buildAccount())
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(text = "  " + stringResource(R.string.delete))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { startDate = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
