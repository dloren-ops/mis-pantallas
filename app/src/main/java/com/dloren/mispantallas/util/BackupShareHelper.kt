package com.dloren.mispantallas.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Envía el respaldo JSON como adjunto a través de la app de correo del usuario, para
 * que cada quien lo guarde en su propia cuenta sin depender de un servidor propio.
 */
object BackupShareHelper {

    suspend fun shareByEmail(context: Context, accounts: List<Account>) {
        val prepared = withContext(Dispatchers.IO) {
            runCatching {
                val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                val dateText = dateFormat.format(Date())
                val file = File(cacheDir, "mispantallas_backup_$dateText.json")
                file.writeText(AccountBackup.toJson(accounts))
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                uri to dateText
            }
        }
        prepared.onSuccess { (uri, dateText) ->
            val subject = context.getString(R.string.backup_email_subject, dateText)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, subject))
        }.onFailure {
            Toast.makeText(context, R.string.backup_email_error, Toast.LENGTH_LONG).show()
        }
    }
}
