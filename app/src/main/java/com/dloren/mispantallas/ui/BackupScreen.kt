package com.dloren.mispantallas.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.util.AccountBackup
import com.dloren.mispantallas.util.BackupShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exportar/importar el respaldo JSON de todas las cuentas, o enviarlo por correo.
 * La lectura/escritura del archivo corre en background (Dispatchers.IO) para no
 * trabar la UI si el respaldo llega a ser grande.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit,
    isReadOnly: Boolean = false
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(AccountBackup.toJson(accounts).toByteArray())
                    }
                }
                Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            AccountBackup.fromJson(stream.bufferedReader().readText())
                        }
                    }.getOrNull()
                }
                if (imported != null) {
                    viewModel.importAccounts(imported)
                    Toast.makeText(context, R.string.import_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.import_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
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
                onClick = { exportLauncher.launch(defaultBackupFileName()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.export_data))
            }
            OutlinedButton(
                onClick = { scope.launch { BackupShareHelper.shareByEmail(context, accounts) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.send_backup_email))
            }
            if (!isReadOnly) {
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.import_data))
                }
            }
        }
    }
}

private fun defaultBackupFileName(): String {
    val format = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
    return "mispantallas_backup_${format.format(Date())}.json"
}
