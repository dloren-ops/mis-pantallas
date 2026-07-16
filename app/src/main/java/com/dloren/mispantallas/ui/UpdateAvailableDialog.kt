package com.dloren.mispantallas.ui

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.util.ApkInstaller

/**
 * Aviso de actualización disponible fuera de Play Store: descarga el APK publicado
 * (Firestore + Storage) y abre el instalador de Android. Siempre opcional/descartable.
 */
@Composable
fun UpdateAvailableDialog(viewModel: AppUpdateViewModel) {
    val update by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val info = update ?: return

    LaunchedEffect(downloadState) {
        when (downloadState) {
            is UpdateDownloadState.NeedsInstallPermission -> {
                Toast.makeText(context, R.string.update_needs_install_permission, Toast.LENGTH_LONG).show()
                ApkInstaller.requestInstallPermission(context)
            }
            is UpdateDownloadState.Error -> {
                Toast.makeText(context, R.string.update_download_error, Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismissUpdate() },
        title = { Text(stringResource(R.string.update_available_title, info.versionName)) },
        text = {
            Text(info.changelog.ifBlank { stringResource(R.string.update_available_message) })
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.downloadAndInstall() },
                enabled = downloadState !is UpdateDownloadState.Downloading
            ) {
                Text(
                    if (downloadState is UpdateDownloadState.Downloading) {
                        stringResource(R.string.update_downloading)
                    } else {
                        stringResource(R.string.update_now)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissUpdate() }) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}
