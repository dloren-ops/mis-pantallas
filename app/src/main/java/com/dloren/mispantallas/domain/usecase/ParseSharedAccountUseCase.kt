package com.dloren.mispantallas.domain.usecase

import com.dloren.mispantallas.domain.model.Account
import java.util.Locale

/**
 * Convierte un texto "a granel" (pegado o compartido) en una [Account], ubicando
 * cada dato en su campo **sin importar el orden** y tolerando varios formatos:
 *
 *  - "etiqueta: valor" o "etiqueta = valor" (ej. `Correo: x@x.com`).
 *  - "etiqueta valor" sin separador (ej. `PIN 1234`, `Clave abc`).
 *  - La **contraseña sin etiqueta** escrita en la línea siguiente al correo.
 *  - Un número suelto de 3 a 6 dígitos se toma como **PIN**.
 *  - El correo se detecta por el "@" y la plataforma por nombre conocido.
 *
 * Lógica pura (sin Android), fácilmente testeable.
 */
class ParseSharedAccountUseCase {

    private enum class Field { EMAIL, PASSWORD, PROFILE, PIN, PHONE, PLATFORM, DURATION }

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

        // Cuando acabamos de detectar el correo, la siguiente línea sin etiqueta
        // se asume que es la contraseña (hábito común al pegar credenciales).
        var expectPasswordNext = false

        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            // 1) Intentar separar "clave: valor" o "clave = valor".
            var key: String? = null
            var value = ""
            val sepIndex = line.indexOfFirst { it == ':' || it == '=' }
            if (sepIndex > 0) {
                key = line.substring(0, sepIndex).trim().lowercase(Locale.getDefault())
                value = line.substring(sepIndex + 1).trim()
            } else {
                // 2) "clave valor" sin separador: la primera palabra es la etiqueta.
                val firstSpace = line.indexOf(' ')
                if (firstSpace > 0) {
                    val firstWord = line.substring(0, firstSpace).trim()
                        .lowercase(Locale.getDefault())
                    if (fieldFor(firstWord) != null) {
                        key = firstWord
                        value = line.substring(firstSpace + 1).trim()
                    }
                }
            }

            val field = key?.let { fieldFor(it) }
            if (field != null && value.isNotEmpty()) {
                when (field) {
                    Field.EMAIL -> {
                        email = value
                        // Si el correo no trae contraseña en la misma línea, la
                        // esperamos en la siguiente.
                        expectPasswordNext = password.isBlank()
                    }
                    Field.PASSWORD -> { password = value; expectPasswordNext = false }
                    Field.PROFILE -> { profile = value; expectPasswordNext = false }
                    Field.PIN -> {
                        pin = value.filter { c -> c.isLetterOrDigit() }
                        expectPasswordNext = false
                    }
                    Field.PHONE -> { clientPhone = value; expectPasswordNext = false }
                    Field.PLATFORM -> { platform = value; expectPasswordNext = false }
                    Field.DURATION -> {
                        Regex("\\d+").find(value)?.value?.toIntOrNull()?.let { duration = it }
                        expectPasswordNext = false
                    }
                }
                matchedAny = true
                return@forEach
            }

            // 3) Línea sin etiqueta reconocida.
            val lower = line.lowercase(Locale.getDefault())
            val isPlatformName = platformKeywords.any { lower.contains(it) }

            when {
                // Contraseña justo debajo del correo (y que no sea la plataforma).
                expectPasswordNext && password.isBlank() && !isPlatformName -> {
                    password = line
                    matchedAny = true
                    expectPasswordNext = false
                }
                // Número suelto de 3 a 6 dígitos -> PIN.
                pin.isBlank() && line.length in 3..6 && line.all { it.isDigit() } -> {
                    pin = line
                    matchedAny = true
                    expectPasswordNext = false
                }
                else -> expectPasswordNext = false
            }
        }

        // 4) Respaldos sin etiqueta.
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

    /** Determina a qué campo corresponde una etiqueta. */
    private fun fieldFor(key: String): Field? = when {
        key.contains("correo") || key.contains("email") || key.contains("mail") ||
            key.contains("usuario") || key.contains("user") -> Field.EMAIL
        key.contains("contrase") || key.contains("password") || key.contains("clave") ||
            key.contains("pass") -> Field.PASSWORD
        key.contains("perfil") || key.contains("profile") -> Field.PROFILE
        key.contains("pin") -> Field.PIN
        key.contains("tel") || key.contains("whats") || key.contains("celular") ||
            key.contains("numero") || key.contains("número") || key.contains("phone") -> Field.PHONE
        key.contains("plataforma") || key.contains("platform") ||
            key.contains("servicio") -> Field.PLATFORM
        key.contains("duraci") || key.contains("dias") || key.contains("días") ||
            key.contains("days") || key.contains("vence") || key.contains("plan") -> Field.DURATION
        else -> null
    }
}
