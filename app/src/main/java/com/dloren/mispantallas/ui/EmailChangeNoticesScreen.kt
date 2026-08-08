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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.util.WhatsAppHelper

/**
 * Pantallas a las que se les cambió el correo y todavía no se les avisó al
 * cliente por WhatsApp (una sola, o varias si se sincronizó el mismo login).
 * Al enviar, se saca de la lista automáticamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailChangeNoticesScreen(
    viewModel: AccountViewModel,
    settingsViewModel: SettingsViewModel,
    onAccountClick: (Long) -> Unit,
    onBack: () -> Unit,
    isReadOnly: Boolean = false
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val pending = remember(accounts) { accounts.filter { it.emailChangeNoticePending } }
    val whatsappFooter by settingsViewModel.whatsappFooter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.email_change_notices_title)) },
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
                accounts = pending,
                isReadOnly = isReadOnly,
                onAccountClick = onAccountClick,
                onSend = { account ->
                    WhatsAppHelper.sendEmailChangeNotice(context, account, whatsappFooter)
                    viewModel.markEmailChangeNoticeSent(account)
                },
                onAssignPhoneAndSend = { account, phone ->
                    val updated = account.copy(clientPhone = phone)
                    WhatsAppHelper.sendEmailChangeNotice(context, updated, whatsappFooter)
                    viewModel.markEmailChangeNoticeSent(updated)
                },
                emptyMessage = stringResource(R.string.no_email_change_notices)
            )
        }
    }
}
