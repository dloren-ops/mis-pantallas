package com.dloren.mispantallas.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R

/**
 * Ajustes de la app: un menú de botones, cada uno abre su propia pantalla
 * (perfiles por defecto, mensaje de WhatsApp, recordatorio de vencimiento, copia de seguridad).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    appUpdateViewModel: AppUpdateViewModel? = null,
    onOpenProfileDefaults: () -> Unit = {},
    onOpenWhatsappMessage: () -> Unit = {},
    onOpenReminder: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenPausedAccounts: () -> Unit = {}
) {
    val context = LocalContext.current
    val manualCheckResult = appUpdateViewModel?.manualCheckResult?.collectAsStateWithLifecycle()?.value

    LaunchedEffect(manualCheckResult) {
        val messageRes = when (manualCheckResult) {
            ManualCheckResult.UpToDate -> R.string.update_up_to_date
            ManualCheckResult.Error -> R.string.update_check_error
            else -> null
        }
        if (messageRes != null) {
            Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
            appUpdateViewModel?.consumeManualCheckResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onOpenProfileDefaults,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_profile_names_section))
            }
            OutlinedButton(
                onClick = onOpenWhatsappMessage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_whatsapp_message_section))
            }
            OutlinedButton(
                onClick = onOpenReminder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_reminder_section))
            }
            OutlinedButton(
                onClick = onOpenBackup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.backup_title))
            }
            OutlinedButton(
                onClick = onOpenPausedAccounts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.paused_accounts_title))
            }
            if (appUpdateViewModel != null) {
                OutlinedButton(
                    onClick = { appUpdateViewModel.checkForUpdateNow() },
                    enabled = manualCheckResult != ManualCheckResult.Checking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (manualCheckResult == ManualCheckResult.Checking) {
                            stringResource(R.string.checking_for_updates)
                        } else {
                            stringResource(R.string.check_for_updates)
                        }
                    )
                }
            }
        }
    }
}
