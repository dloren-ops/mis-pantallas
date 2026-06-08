package com.dloren.mispantallas.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dloren.mispantallas.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestiona la búsqueda, descarga e instalación de actualizaciones publicadas como
 * "Releases" en GitHub.
 *
 * Esquema de versiones: el workflow de CI etiqueta cada release como
 * `v<versionName>.<numero>` (ej. `v1.0.42`), donde `<numero>` es el versionCode
 * con el que se compiló el APK. Así podemos comparar el último release contra el
 * versionCode instalado ([BuildConfig.VERSION_CODE]).
 */
object UpdateManager {

    data class Release(
        val versionCode: Int,
        val tag: String,
        val notes: String,
        val apkUrl: String
    )

    sealed interface CheckResult {
        data class UpdateAvailable(val release: Release) : CheckResult
        data object UpToDate : CheckResult
        data class Error(val message: String) : CheckResult
    }

    private const val TIMEOUT_MS = 15_000

    /** Consulta el último release y decide si hay una versión más nueva. */
    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatestRelease()
                ?: return@withContext CheckResult.Error("No hay versiones publicadas todavía.")
            if (release.versionCode > BuildConfig.VERSION_CODE) {
                CheckResult.UpdateAvailable(release)
            } else {
                CheckResult.UpToDate
            }
        } catch (e: Exception) {
            CheckResult.Error(e.message ?: "Error desconocido al buscar actualizaciones.")
        }
    }

    private fun fetchLatestRelease(): Release? {
        val url = URL(
            "https://api.github.com/repos/" +
                "${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MisPantallas-Updater")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            val notes = json.optString("body")

            // versionCode = último segmento numérico del tag (v1.0.42 -> 42).
            val versionCode = tag.substringAfterLast('.').filter { it.isDigit() }
                .toIntOrNull() ?: return null

            // Buscar el primer asset .apk.
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return null

            return Release(versionCode, tag, notes, apkUrl)
        } finally {
            conn.disconnect()
        }
    }

    /** Descarga el APK del release a la carpeta de actualizaciones de la app. */
    suspend fun downloadApk(context: Context, release: Release): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            // Limpiar APKs previos.
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
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                conn.disconnect()
            }
            outFile
        }

    /**
     * Lanza el instalador del sistema para el APK descargado. En Android 8+ pide
     * permiso de "instalar apps desconocidas" si aún no se concedió.
     */
    fun installApk(context: Context, apk: File) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            // Llevar al usuario a habilitar la instalación desde esta app.
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return
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
    }
}
