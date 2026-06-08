package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import java.util.Locale

/**
 * Convierte un texto "a granel" (pegado o compartido desde WhatsApp) en una
 * [Account], ubicando cada dato en su campo **sin importar el orden**.
 *
 * Estrategia:
 *  1. Por cada línea con formato "etiqueta: valor" (también acepta "=" y "-")
 *     asigna el valor al campo según la etiqueta (correo, contraseña, perfil,
 *     PIN, plataforma, teléfono, duración).
 *  2. Como respaldo, escanea todo el texto para detectar datos aunque NO tengan
 *     etiqueta: un correo (por el "@"), una plataforma conocida (Netflix,
 *     Disney, etc.) y un teléfono largo.
 *
 * Lógica pura (sin dependencias de Android), por lo que es fácilmente testeable.
 */
class ParseSharedAccountUseCase {

    private val platformKeywords = listOf(
        "netflix", "disney", "hbo", "max", "prime", "amazon", "star", "paramount",
        "spotify", "youtube", "crunchyroll", "vix", "apple tv", "appletv", "deezer",
        "directv", "plex", "iptv"
    )

    private val emailRegex = Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")

    operator fun invoke(text: String?): Account? {
        if (text.isNullOrBlank()) return null

        var email = ""
        var password = ""
        var profile = ""
        var pin = ""
        var platform = ""
        var clientPhone = ""
        var duration: Int? = null
        var matchedAny = false

        // --- Paso 1: líneas "etiqueta: valor" ---
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            val sepIndex = line.indexOfFirst { it == ':' || it == '=' }
            if (sepIndex <= 0) return@forEach
            val key = line.substring(0, sepIndex).trim().lowercase(Locale.getDefault())
            val value = line.substring(sepIndex + 1).trim()
            if (value.isEmpty()) return@forEach

            when {
                key.contains("correo") || key.contains("email") || key.contains("mail") ||
                    key.contains("usuario") || key.contains("user") -> {
                    email = value; matchedAny = true
                }
                key.contains("contrase") || key.contains("password") || key.contains("clave") ||
                    key.contains("pass") -> {
                    password = value; matchedAny = true
                }
                key.contains("perfil") || key.contains("profile") -> {
                    profile = value; matchedAny = true
                }
                key.contains("pin") -> {
                    pin = value; matchedAny = true
                }
                key.contains("tel") || key.contains("whats") || key.contains("celular") ||
                    key.contains("numero") || key.contains("número") || key.contains("phone") -> {
                    clientPhone = value; matchedAny = true
                }
                key.contains("plataforma") || key.contains("platform") ||
                    key.contains("servicio") || key.contains("cuenta") -> {
                    platform = value; matchedAny = true
                }
                key.contains("duraci") || key.contains("dias") || key.contains("días") ||
                    key.contains("days") || key.contains("vence") || key.contains("plan") -> {
                    Regex("\\d+").find(value)?.value?.toIntOrNull()?.let {
                        duration = it; matchedAny = true
                    }
                }
            }
        }

        // --- Paso 2: respaldo sin etiquetas ---
        if (email.isBlank()) {
            emailRegex.find(text)?.value?.let { email = it; matchedAny = true }
        }
        if (platform.isBlank()) {
            val lower = text.lowercase(Locale.getDefault())
            platformKeywords.firstOrNull { lower.contains(it) }?.let {
                platform = it.replaceFirstChar { c -> c.uppercase() }
                matchedAny = true
            }
        }

        if (!matchedAny) return null

        return Account(
            email = email,
            password = password,
            profileName = profile,
            pin = pin,
            platform = platform,
            clientPhone = clientPhone,
            durationDays = duration ?: 30
        )
    }
}
