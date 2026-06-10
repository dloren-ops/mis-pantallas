package com.dloren.mispantallas.presentation.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Operaciones de archivo para el respaldo: compartir un JSON y leer uno elegido. */
object BackupIo {

    /** Escribe el JSON en un archivo de cache y abre el menú de compartir. */
    fun shareJson(context: Context, json: String) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "mis-pantallas-respaldo-$stamp.json")
        file.writeText(json)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Guardar respaldo"))
    }

    /** Lee el contenido de texto del archivo elegido por el usuario. */
    fun readText(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: ""
    }
}
