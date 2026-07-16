package com.dloren.mispantallas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.util.WhatsAppHelper

/**
 * Agrupa las pantallas alquiladas por número de WhatsApp del cliente, para ver de
 * un vistazo cuántas pantallas tiene cada uno y renovarlas/enviarlas juntas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: AccountViewModel,
    settingsViewModel: SettingsViewModel,
    onAccountClick: (Long) -> Unit,
    onBack: () -> Unit,
    isReadOnly: Boolean = false
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val whatsappMessage by settingsViewModel.whatsappMessage.collectAsStateWithLifecycle()
    val whatsappFooter by settingsViewModel.whatsappFooter.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val groups = remember(accounts, searchQuery) {
        val query = searchQuery.trim()
        accounts
            .filter { it.clientPhone.isNotBlank() }
            .filter { account ->
                query.isEmpty() ||
                    account.clientPhone.contains(query, ignoreCase = true) ||
                    account.email.contains(query, ignoreCase = true)
            }
            .groupBy { it.clientPhone }
            .toList()
            .sortedBy { (_, clientAccounts) -> clientAccounts.minOf { it.remainingDays() } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clients_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isReadOnly) {
                ReadOnlyBanner()
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_clients),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                val expanded = remember { mutableStateMapOf<String, Boolean>() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups, key = { it.first }) { (phone, clientAccounts) ->
                        ClientGroupCard(
                            phone = phone,
                            accounts = clientAccounts,
                            isExpanded = expanded[phone] ?: false,
                            onToggle = { expanded[phone] = !(expanded[phone] ?: false) },
                            onAccountClick = onAccountClick,
                            onSend = { account -> WhatsAppHelper.sendToWhatsApp(context, account, whatsappMessage, whatsappFooter) },
                            onDeleteClient = if (isReadOnly) null else {
                                { viewModel.deleteAll(clientAccounts) }
                            },
                            isReadOnly = isReadOnly
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientGroupCard(
    phone: String,
    accounts: List<Account>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onSend: (Account) -> Unit,
    onDeleteClient: (() -> Unit)? = null,
    isReadOnly: Boolean
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.screens_count, accounts.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDeleteClient != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        AccountCard(
                            account = account,
                            onClick = { onAccountClick(account.id) },
                            onSend = { onSend(account) },
                            isReadOnly = isReadOnly
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_client_title)) },
            text = { Text(stringResource(R.string.delete_client_confirm, accounts.size)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClient?.invoke()
                    showDeleteConfirm = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}
