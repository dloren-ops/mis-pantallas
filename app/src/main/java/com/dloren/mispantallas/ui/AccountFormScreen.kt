package com.dloren.mispantallas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.data.Payment
import com.dloren.mispantallas.util.DAY_PRESETS
import com.dloren.mispantallas.util.DateUtils
import com.dloren.mispantallas.util.WhatsAppHelper
import com.dloren.mispantallas.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountFormScreen(
    accountId: Long,
    prefill: Account?,
    viewModel: AccountViewModel,
    settingsViewModel: SettingsViewModel,
    paymentViewModel: PaymentViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    isReadOnly: Boolean = false
) {
    val context = LocalContext.current
    val isEditing = accountId > 0L
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val whatsappMessage by settingsViewModel.whatsappMessage.collectAsStateWithLifecycle()
    val whatsappFooter by settingsViewModel.whatsappFooter.collectAsStateWithLifecycle()
    val paymentsFlow = remember(accountId) { paymentViewModel.paymentsFor(accountId) }
    val payments by paymentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var siblingsToSync by remember { mutableStateOf<List<Account>>(emptyList()) }

    // Estado del formulario.
    var loaded by rememberSaveable(accountId) { mutableStateOf(false) }
    var originalEmail by rememberSaveable(accountId) { mutableStateOf(prefill?.email ?: "") }
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
        mutableStateOf(prefill?.startDateMillis ?: DateUtils.nowStartOfDayMillis())
    }
    var price by rememberSaveable(accountId) {
        mutableStateOf(prefill?.price?.takeIf { it > 0.0 }?.toString() ?: "")
    }
    var renewEveryDaysText by rememberSaveable(accountId) {
        mutableStateOf((prefill?.renewEveryDays ?: 0).takeIf { it > 0 }?.toString() ?: "")
    }
    var providerStartMillis by rememberSaveable(accountId) {
        mutableStateOf(prefill?.providerStartMillis ?: 0L)
    }
    // Campos que hoy no se editan desde el formulario, pero que hay que conservar
    // tal cual si la cuenta vino de un respaldo importado (no reiniciarlos al guardar).
    var status by rememberSaveable(accountId) { mutableStateOf(prefill?.status ?: "SOLD") }
    var soldDateMillis by rememberSaveable(accountId) { mutableStateOf(prefill?.soldDateMillis ?: 0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showProviderDatePicker by remember { mutableStateOf(false) }

    // Cargar la cuenta existente cuando se edita.
    LaunchedEffect(accountId) {
        if (isEditing && !loaded) {
            viewModel.getAccount(accountId)?.let { acc ->
                originalEmail = acc.email
                email = acc.email
                password = acc.password
                profile = acc.profileName
                pin = acc.pin
                platform = acc.platform
                clientPhone = acc.clientPhone
                duration = acc.durationDays.toString()
                startDate = acc.startDateMillis
                price = if (acc.price > 0.0) acc.price.toString() else ""
                renewEveryDaysText = acc.renewEveryDays.takeIf { it > 0 }?.toString() ?: ""
                providerStartMillis = acc.providerStartMillis
                status = acc.status
                soldDateMillis = acc.soldDateMillis
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
        startDateMillis = startDate,
        status = status,
        soldDateMillis = soldDateMillis,
        renewEveryDays = renewEveryDaysText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        providerStartMillis = providerStartMillis,
        price = price.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
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
            if (isReadOnly) {
                ReadOnlyBanner()
            }
            PlatformSelector(
                platform = platform,
                onPlatformChange = { platform = it },
                enabled = !isReadOnly,
                modifier = Modifier.fillMaxWidth()
            )
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
                value = profile,
                onValueChange = { profile = it },
                label = { Text(stringResource(R.string.field_profile)) },
                singleLine = true,
                readOnly = isReadOnly,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text(stringResource(R.string.field_pin)) },
                singleLine = true,
                readOnly = isReadOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                label = { Text(stringResource(R.string.field_client_phone)) },
                singleLine = true,
                readOnly = isReadOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.field_duration_quick),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selectedDays = duration.toIntOrNull()
                DAY_PRESETS.forEach { days ->
                    FilterChip(
                        selected = selectedDays == days,
                        enabled = !isReadOnly,
                        onClick = { duration = days.toString() },
                        label = { Text("$days días") }
                    )
                }
            }
            OutlinedTextField(
                value = duration,
                onValueChange = { new -> duration = new.filter { it.isDigit() } },
                label = { Text(stringResource(R.string.field_duration)) },
                singleLine = true,
                readOnly = isReadOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dateFormat.format(Date(startDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_start_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }, enabled = !isReadOnly) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = price,
                onValueChange = { new ->
                    price = new.filterIndexed { index, c -> c.isDigit() || (c == '.' && new.indexOf('.') == index) }
                },
                label = { Text(stringResource(R.string.field_price)) },
                singleLine = true,
                readOnly = isReadOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.field_provider_section),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(R.string.field_provider_section_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = renewEveryDaysText,
                onValueChange = { new -> renewEveryDaysText = new.filter { it.isDigit() } },
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
                    stringResource(R.string.provider_start_same_as_client)
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
                        IconButton(onClick = { showProviderDatePicker = true }, enabled = !isReadOnly) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            buildAccount().providerRemainingDays()?.let { days ->
                CountdownLabel(days, isProviderCountdown = true)
            }

            if (!isReadOnly) {
                Button(
                    onClick = {
                        val newEmail = email.trim()
                        val siblings = if (isEditing && newEmail.isNotBlank() &&
                            originalEmail.isNotBlank() &&
                            !newEmail.equals(originalEmail.trim(), ignoreCase = true)
                        ) {
                            allAccounts.filter {
                                it.id != accountId && it.email.equals(originalEmail.trim(), ignoreCase = true)
                            }
                        } else {
                            emptyList()
                        }
                        if (siblings.isNotEmpty()) {
                            siblingsToSync = siblings
                        } else {
                            viewModel.save(buildAccount()) { onDone() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }

                OutlinedButton(
                    onClick = { WhatsAppHelper.sendToWhatsApp(context, buildAccount(), whatsappMessage, whatsappFooter) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Text(
                        text = "  " + stringResource(R.string.send_whatsapp)
                    )
                }

                if (isEditing) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text(text = "  " + stringResource(R.string.delete))
                    }
                }
            }

            if (isEditing) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.payments_section_title),
                    style = MaterialTheme.typography.labelLarge
                )
                if (payments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_payments),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    payments.forEach { payment ->
                        PaymentRow(
                            payment = payment,
                            dateFormat = dateFormat,
                            isReadOnly = isReadOnly,
                            onDelete = { paymentViewModel.delete(payment) }
                        )
                    }
                }
                if (!isReadOnly) {
                    OutlinedButton(
                        onClick = { showAddPaymentDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(text = "  " + stringResource(R.string.add_payment))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toPickerMillis(startDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { startDate = DateUtils.fromPickerMillis(it) }
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

    if (showProviderDatePicker) {
        val providerPickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toPickerMillis(
                if (providerStartMillis > 0) providerStartMillis else startDate
            )
        )
        DatePickerDialog(
            onDismissRequest = { showProviderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    providerPickerState.selectedDateMillis?.let {
                        providerStartMillis = DateUtils.fromPickerMillis(it)
                    }
                    showProviderDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showProviderDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = providerPickerState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_account_confirm_title)) },
            text = { Text(stringResource(R.string.delete_account_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(buildAccount())
                    showDeleteConfirm = false
                    onDone()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (siblingsToSync.isNotEmpty()) {
        val newEmail = email.trim()
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.email_sync_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.email_sync_confirm_message, siblingsToSync.size, newEmail)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val siblings = siblingsToSync
                    siblingsToSync = emptyList()
                    viewModel.save(buildAccount()) {
                        viewModel.updateEmailForAccounts(siblings, newEmail)
                        onDone()
                    }
                }) { Text(stringResource(R.string.email_sync_yes)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    siblingsToSync = emptyList()
                    viewModel.save(buildAccount()) { onDone() }
                }) { Text(stringResource(R.string.email_sync_no)) }
            }
        )
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            onDismiss = { showAddPaymentDialog = false },
            onSave = { amount, dateMillis, note ->
                paymentViewModel.addPayment(accountId, amount, dateMillis, note)
                showAddPaymentDialog = false
            }
        )
    }
}

@Composable
private fun PaymentRow(
    payment: Payment,
    dateFormat: SimpleDateFormat,
    isReadOnly: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatCurrency(payment.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (payment.note.isNotBlank()) {
                    "${dateFormat.format(Date(payment.dateMillis))} · ${payment.note}"
                } else {
                    dateFormat.format(Date(payment.dateMillis))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isReadOnly) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, dateMillis: Long, note: String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(DateUtils.nowStartOfDayMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_payment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { new ->
                        amount = new.filterIndexed { index, c -> c.isDigit() || (c == '.' && new.indexOf('.') == index) }
                    },
                    label = { Text(stringResource(R.string.field_payment_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateFormat.format(Date(dateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_payment_date)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_payment_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(amount.toDoubleOrNull() ?: 0.0, dateMillis, note.trim()) },
                enabled = amount.toDoubleOrNull() != null
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = DateUtils.toPickerMillis(dateMillis))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = DateUtils.fromPickerMillis(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
