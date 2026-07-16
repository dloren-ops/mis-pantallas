package com.dloren.mispantallas.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga e instala una actualización distribuida como APK propio (fuera de Play Store).
 * Requiere que el usuario habilite "instalar apps de origen desconocido" para esta app.
 */
object ApkInstaller {

    fun canInstallApks(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Lleva al usuario a la pantalla de Ajustes donde puede habilitar la instalación. */
    fun requestInstallPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    suspend fun downloadApk(context: Context, apkUrl: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val file = File(dir, "mispantallas_update.apk")
        val connection = URL(apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.connect()
        check(connection.responseCode == HttpURLConnection.HTTP_OK) {
            "HTTP ${connection.responseCode}"
        }
        connection.inputStream.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        connection.disconnect()
        file
    }

    /** Abre el instalador de paquetes de Android sobre el APK ya descargado. */
    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
