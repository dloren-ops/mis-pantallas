package com.dloren.mispantallas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.util.WhatsAppHelper

/**
 * Pantallas que hay que renovar con el proveedor (Netflix, Disney+, etc.), en
 * orden de vencimiento de ese ciclo, para no olvidarse de pagarlas/renovarlas
 * cada cierta cantidad de días (1 a 30, según lo que se configure por cuenta).
 * No tiene que ver con el ciclo de alquiler con el cliente, que se maneja
 * desde "Mis Pantallas".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewalsScreen(
    viewModel: AccountViewModel,
    settingsViewModel: SettingsViewModel,
    onAccountClick: (Long) -> Unit,
    onBack: () -> Unit,
    isReadOnly: Boolean = false
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val renewals = remember(accounts) {
        accounts
            .filter { it.clientPhone.isNotBlank() && it.renewEveryDays > 0 }
            .sortedBy { it.providerRemainingDays() ?: Long.MAX_VALUE }
    }
    val whatsappMessage by settingsViewModel.whatsappMessage.collectAsStateWithLifecycle()
    val whatsappFooter by settingsViewModel.whatsappFooter.collectAsStateWithLifecycle()
    val renewalReminderMessage by settingsViewModel.renewalReminderMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.renewals_title)) },
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
            AccountsColumn(
                accounts = renewals,
                isReadOnly = isReadOnly,
                onAccountClick = onAccountClick,
                onSend = { account -> WhatsAppHelper.sendToWhatsApp(context, account, whatsappMessage, whatsappFooter) },
                onAssignPhoneAndSend = { account, phone ->
                    val updated = account.copy(clientPhone = phone)
                    viewModel.save(updated) { WhatsAppHelper.sendToWhatsApp(context, updated, whatsappMessage, whatsappFooter) }
                },
                onSendRenewalReminder = { account ->
                    WhatsAppHelper.sendRenewalReminder(context, account, renewalReminderMessage)
                },
                useProviderCountdown = true,
                emptyMessage = stringResource(R.string.no_renewals)
            )
        }
    }
}
