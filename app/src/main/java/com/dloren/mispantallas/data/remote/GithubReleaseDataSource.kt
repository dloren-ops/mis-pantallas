package com.dloren.mispantallas.data.remote

import com.dloren.mispantallas.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fuente de datos remota que consulta la API de GitHub para obtener el último
 * release. Usa HttpURLConnection para no sumar dependencias (mantiene la app
 * liviana).
 */
class GithubReleaseDataSource(
    private val owner: String,
    private val repo: String
) {
    private companion object {
        const val TIMEOUT_MS = 15_000
    }

    suspend fun fetchLatestRelease(): AppRelease? = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MisPantallas-Updater")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseRelease(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRelease(body: String): AppRelease? {
        val json = JSONObject(body)
        val tag = json.optString("tag_name")
        val notes = json.optString("body")

        // versionCode = último segmento numérico del tag (v1.0.42 -> 42).
        val versionCode = tag.substringAfterLast('.').filter { it.isDigit() }
            .toIntOrNull() ?: return null

        val assets = json.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
        if (apkUrl.isNullOrBlank()) return null

        return AppRelease(versionCode = versionCode, tag = tag, notes = notes, apkUrl = apkUrl)
    }
}
