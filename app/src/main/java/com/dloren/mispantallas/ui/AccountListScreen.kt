package com.dloren.mispantallas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.util.UpdateManager
import com.dloren.mispantallas.util.WhatsAppHelper
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    viewModel: AccountViewModel,
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado del flujo de actualización.
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<UpdateManager.Release?>(null) }

    fun checkForUpdates() {
        if (checking || downloading) return
        checking = true
        scope.launch {
            when (val result = UpdateManager.check()) {
                is UpdateManager.CheckResult.UpdateAvailable -> available = result.release
                is UpdateManager.CheckResult.UpToDate ->
                    Toast.makeText(context, R.string.up_to_date, Toast.LENGTH_SHORT).show()
                is UpdateManager.CheckResult.Error ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.update_error, result.message),
                        Toast.LENGTH_LONG
                    ).show()
            }
            checking = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_accounts)) },
                actions = {
                    if (checking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { checkForUpdates() }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.check_updates)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_account))
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_accounts),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onClick = { onAccountClick(account.id) },
                        onSend = { WhatsAppHelper.sendToWhatsApp(context, account) }
                    )
                }
            }
        }
    }

    // Diálogo cuando hay una actualización disponible.
    val release = available
    if (release != null) {
        AlertDialog(
            onDismissRequest = { if (!downloading) available = null },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                if (downloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "  " + stringResource(R.string.downloading_update)
                        )
                    }
                } else {
                    Text(stringResource(R.string.update_available_body, release.tag))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !downloading,
                    onClick = {
                        downloading = true
                        scope.launch {
                            try {
                                val apk = UpdateManager.downloadApk(context, release)
                                UpdateManager.installApk(context, apk)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.update_error,
                                        e.message ?: ""
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                downloading = false
                                available = null
                            }
                        }
                    }
                ) { Text(stringResource(R.string.update_now)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !downloading,
                    onClick = { available = null }
                ) { Text(stringResource(R.string.later)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    onSend: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.platform.ifBlank { account.email.ifBlank { "Cuenta" } },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (account.profileName.isNotBlank()) {
                    Text(
                        text = "Perfil: ${account.profileName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (account.email.isNotBlank()) {
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CountdownLabel(account)
            }
            IconButton(onClick = onSend) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_whatsapp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CountdownLabel(account: Account) {
    val remaining = account.remainingDays()
    val (text, color) = when {
        remaining < 0L -> stringResource(R.string.expired_ago, -remaining) to Color(0xFFB3261E)
        remaining == 0L -> stringResource(R.string.last_day) to Color(0xFFB3261E)
        remaining == 1L -> stringResource(R.string.one_day_left) to Color(0xFFE65100)
        remaining <= 3L -> stringResource(R.string.days_left, remaining) to Color(0xFFE65100)
        else -> stringResource(R.string.days_left, remaining) to Color(0xFF2E7D32)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 6.dp)
    )
}
