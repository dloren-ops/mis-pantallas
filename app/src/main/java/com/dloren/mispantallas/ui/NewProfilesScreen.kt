package com.dloren.mispantallas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.util.DateUtils
import com.dloren.mispantallas.util.WhatsAppHelper

/**
 * Perfiles generados en bloque que todavía no se asignaron a ningún cliente
 * (sin WhatsApp cargado), listos para editar y enviar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProfilesScreen(
    viewModel: AccountViewModel,
    settingsViewModel: SettingsViewModel,
    onAccountClick: (Long) -> Unit,
    onBack: () -> Unit,
    isReadOnly: Boolean = false
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val newAccounts = remember(accounts) { accounts.filter { it.clientPhone.isBlank() } }
    var selectedPlatform by remember { mutableStateOf<String?>(null) }
    val filteredAccounts = remember(newAccounts, selectedPlatform) {
        if (selectedPlatform == null) newAccounts else newAccounts.filter { it.platform == selectedPlatform }
    }
    val whatsappMessage by settingsViewModel.whatsappMessage.collectAsStateWithLifecycle()
    val whatsappFooter by settingsViewModel.whatsappFooter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_profiles_title)) },
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
            PlatformFilterRow(
                accounts = newAccounts,
                selectedPlatform = selectedPlatform,
                onSelectPlatform = { selectedPlatform = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            AccountsColumn(
                accounts = filteredAccounts,
                isReadOnly = isReadOnly,
                onAccountClick = onAccountClick,
                onSend = { account -> WhatsAppHelper.sendToWhatsApp(context, account, whatsappMessage, whatsappFooter) },
                onAssignPhoneAndSend = { account, phone ->
                    val updated = account.copy(clientPhone = phone, startDateMillis = DateUtils.nowStartOfDayMillis())
                    viewModel.save(updated) { WhatsAppHelper.sendToWhatsApp(context, updated, whatsappMessage, whatsappFooter) }
                },
                allowStatusToggle = true,
                onMarkAsRented = { account, phone ->
                    viewModel.save(account.copy(clientPhone = phone, startDateMillis = DateUtils.nowStartOfDayMillis()))
                },
                onMarkAsNew = { account -> viewModel.save(account.copy(clientPhone = "")) },
                emptyMessage = stringResource(R.string.no_new_profiles)
            )
        }
    }
}
