package com.dloren.mispantallas.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.ClientLicense
import com.dloren.mispantallas.util.MONTH_PRESETS
import com.dloren.mispantallas.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Panel de administración: alta y renovación de licencias de clientes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: LicenseViewModel, onBack: () -> Unit) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val adminError by viewModel.adminError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<ClientLicense?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshClients() }

    LaunchedEffect(adminError) {
        adminError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearAdminError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.admin_new_client))
            }
        }
    ) { padding ->
        if (clients.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.admin_no_clients))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clients, key = { it.code }) { client ->
                    ClientCard(
                        client = client,
                        onExtend = { months -> viewModel.extendClient(client.code, months) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateClientDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, phone ->
                viewModel.createClient(name, phone) { license -> generatedCode = license }
                showCreateDialog = false
            }
        )
    }

    generatedCode?.let { license ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { generatedCode = null },
            text = {
                Column {
                    Text(stringResource(R.string.admin_client_code_generated))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = license.code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(license.code))
                            Toast.makeText(context, R.string.code_copied, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    WhatsAppHelper.sendText(
                        context,
                        license.phone,
                        "Tu código de acceso a Mis Pantallas es: ${license.code}"
                    )
                    generatedCode = null
                }) { Text(stringResource(R.string.send_whatsapp)) }
            },
            dismissButton = {
                TextButton(onClick = { generatedCode = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientCard(client: ClientLicense, onExtend: (Int) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = client.name.ifBlank { client.code },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = client.code, style = MaterialTheme.typography.bodySmall)
            if (client.phone.isNotBlank()) {
                Text(text = client.phone, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = if (client.expiresAtMillis == 0L) {
                    stringResource(R.string.admin_client_never_activated)
                } else {
                    stringResource(
                        R.string.admin_client_expires,
                        dateFormat.format(Date(client.expiresAtMillis))
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (client.boundDeviceId.isNotBlank()) {
                Text(
                    text = stringResource(R.string.admin_client_device_bound),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                MONTH_PRESETS.forEach { (months, label) ->
                    OutlinedButton(onClick = { onExtend(months) }) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateClientDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_new_client)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.admin_client_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.admin_client_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), phone.trim()) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.admin_client_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
