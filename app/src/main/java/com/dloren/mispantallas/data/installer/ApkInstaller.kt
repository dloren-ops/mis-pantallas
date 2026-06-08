package com.dloren.mispantallas.data.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dloren.mispantallas.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga el APK de un release e invoca al instalador del sistema.
 * Encapsula las operaciones que dependen del framework de Android.
 */
class ApkInstaller(private val context: Context) {

    private companion object {
        const val TIMEOUT_MS = 30_000
    }

    /** Descarga el APK del release a la carpeta de actualizaciones de la app. */
    suspend fun download(release: AppRelease): File = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        val outFile = File(dir, "mis-pantallas-${release.tag}.apk")
        val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MisPantallas-Updater")
        }
        try {
            conn.inputStream.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            conn.disconnect()
        }
        outFile
    }

    /**
     * Lanza el instalador del sistema. En Android 8+ deriva a Ajustes si todavía
     * no se concedió el permiso de instalar apps de orígenes desconocidos.
     *
     * @return true si lanzó el instalador; false si tuvo que pedir el permiso.
     */
    fun install(apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(install)
        return true
    }
}
