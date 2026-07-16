package com.dloren.mispantallas.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R

/** Encabezado y mensaje final personalizados para cuando se envían datos por WhatsApp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsappMessageScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val storedMessage by viewModel.whatsappMessage.collectAsStateWithLifecycle()
    val storedFooter by viewModel.whatsappFooter.collectAsStateWithLifecycle()
    val storedRenewalReminder by viewModel.renewalReminderMessage.collectAsStateWithLifecycle()

    var message by remember { mutableStateOf("") }
    var footer by remember { mutableStateOf("") }
    var renewalReminder by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(storedMessage, storedFooter, storedRenewalReminder) {
        if (!loaded) {
            message = storedMessage
            footer = storedFooter
            renewalReminder = storedRenewalReminder
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_whatsapp_message_section)) },
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
            Text(
                text = stringResource(R.string.settings_whatsapp_message_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text(stringResource(R.string.field_whatsapp_header)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.settings_whatsapp_footer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = footer,
                onValueChange = { footer = it },
                label = { Text(stringResource(R.string.field_whatsapp_footer)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.settings_renewal_reminder_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.settings_renewal_reminder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = renewalReminder,
                onValueChange = { renewalReminder = it },
                label = { Text(stringResource(R.string.field_renewal_reminder_message)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.saveWhatsappMessage(message.trim())
                    viewModel.saveWhatsappFooter(footer.trim())
                    viewModel.saveRenewalReminderMessage(renewalReminder.trim())
                    Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
