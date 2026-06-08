package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import java.util.Locale

/**
 * Convierte un texto compartido (p. ej. desde WhatsApp) en una [Account].
 *
 * Reconoce el formato "etiqueta: valor" por línea que genera la propia app, y
 * también variantes comunes (correo/email, contraseña/clave, etc.). Lógica pura,
 * sin dependencias de Android.
 */
class ParseSharedAccountUseCase {

    operator fun invoke(text: String?): Account? {
        if (text.isNullOrBlank()) return null

        var email = ""
        var password = ""
        var profile = ""
        var pin = ""
        var platform = ""
        var duration: Int? = null
        var matchedAny = false

        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            val sepIndex = line.indexOf(':')
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
                key.contains("plataforma") || key.contains("platform") ||
                    key.contains("servicio") -> {
                    platform = value; matchedAny = true
                }
                key.contains("duraci") || key.contains("dias") || key.contains("días") ||
                    key.contains("days") -> {
                    Regex("\\d+").find(value)?.value?.toIntOrNull()?.let {
                        duration = it; matchedAny = true
                    }
                }
            }
        }

        if (!matchedAny) return null

        return Account(
            email = email,
            password = password,
            profileName = profile,
            pin = pin,
            platform = platform,
            durationDays = duration ?: 30
        )
    }
}
