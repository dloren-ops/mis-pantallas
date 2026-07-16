package com.dloren.mispantallas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Edición acotada desde la pestaña Renovaciones: solo los datos que hacen
 * falta para renovar la cuenta con el proveedor (correo, contraseña, cada
 * cuántos días toca renovar y la fecha de inicio de ese ciclo). No expone el
 * WhatsApp del cliente ni el ciclo de alquiler con el cliente, que se maneja
 * desde "Mis Pantallas".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewalEditScreen(
    accountId: Long,
    viewModel: AccountViewModel,
    pausedAccountsViewModel: PausedAccountsViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    isReadOnly: Boolean = false
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    var loaded by rememberSaveable(accountId) { mutableStateOf(false) }
    var original by remember(accountId) { mutableStateOf<Account?>(null) }
    var email by rememberSaveable(accountId) { mutableStateOf("") }
    var password by rememberSaveable(accountId) { mutableStateOf("") }
    var renewEveryDaysText by rememberSaveable(accountId) { mutableStateOf("") }
    var providerStartMillis by rememberSaveable(accountId) { mutableStateOf(0L) }
    var startDateMillis by rememberSaveable(accountId) { mutableStateOf(DateUtils.nowStartOfDayMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        if (!loaded) {
            viewModel.getAccount(accountId)?.let { acc ->
                original = acc
                email = acc.email
                password = acc.password
                renewEveryDaysText = acc.renewEveryDays.takeIf { it > 0 }?.toString() ?: ""
                providerStartMillis = acc.providerStartMillis
                startDateMillis = acc.startDateMillis
            }
            loaded = true
        }
    }

    fun buildUpdated(acc: Account): Account = acc.copy(
        email = email.trim(),
        password = password.trim(),
        renewEveryDays = renewEveryDaysText.toIntOrNull()?.coerceIn(1, 30) ?: 0,
        providerStartMillis = providerStartMillis
    )

    /** La misma cuenta con el proveedor suele tener varios perfiles (una fila por perfil). */
    fun sameEmailAccounts(acc: Account): List<Account> =
        if (acc.email.isBlank()) {
            listOf(acc)
        } else {
            allAccounts.filter { it.email.equals(acc.email, ignoreCase = true) }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.renewal_edit_title)) },
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
            if (isReadOnly) {
                ReadOnlyBanner()
            }
            original?.let { acc ->
                Text(
                    text = acc.platform.ifBlank { acc.email.ifBlank { "Cuenta" } },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.field_email)) },
                singleLine = true,
                readOnly = isReadOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.field_password)) },
                singleLine = true,
                readOnly = isReadOnly,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = renewEveryDaysText,
                onValueChange = { new -> renewEveryDaysText = new.filter { it.isDigit() }.take(2) },
                label = { Text(stringResource(R.string.field_renew_every_days)) },
                singleLine = true,
                readOnly = isReadOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = if (providerStartMillis > 0) {
                    dateFormat.format(Date(providerStartMillis))
                } else {
                    dateFormat.format(Date(startDateMillis))
                },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_provider_start_date)) },
                trailingIcon = {
                    Row {
                        if (providerStartMillis > 0 && !isReadOnly) {
                            IconButton(onClick = { providerStartMillis = 0L }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.provider_start_clear))
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }, enabled = !isReadOnly) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            original?.let { acc ->
                buildUpdated(acc).providerRemainingDays()?.let { days ->
                    CountdownLabel(days, isProviderCountdown = true)
                }
            }

            if (!isReadOnly) {
                Button(
                    onClick = {
                        original?.let { acc -> viewModel.save(buildUpdated(acc)) { onDone() } }
                    },
                    enabled = original != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
                OutlinedButton(
                    onClick = { showPauseConfirm = true },
                    enabled = original != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PauseCircle, contentDescription = null)
                    Text(text = "  " + stringResource(R.string.renewal_pause_action))
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = original != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(text = "  " + stringResource(R.string.delete))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toPickerMillis(
                if (providerStartMillis > 0) providerStartMillis else startDateMillis
            )
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { providerStartMillis = DateUtils.fromPickerMillis(it) }
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.renewal_delete_confirm_title)) },
            text = { Text(stringResource(R.string.renewal_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    original?.let { acc -> viewModel.deleteAll(sameEmailAccounts(acc)) }
                    showDeleteConfirm = false
                    onDone()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showPauseConfirm) {
        AlertDialog(
            onDismissRequest = { showPauseConfirm = false },
            title = { Text(stringResource(R.string.renewal_pause_confirm_title)) },
            text = { Text(stringResource(R.string.renewal_pause_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    original?.let { acc ->
                        pausedAccountsViewModel.save(acc.email.trim(), acc.password.trim(), acc.platform.trim())
                        viewModel.deleteAll(sameEmailAccounts(acc))
                    }
                    showPauseConfirm = false
                    onDone()
                }) { Text(stringResource(R.string.renewal_pause_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showPauseConfirm = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}
