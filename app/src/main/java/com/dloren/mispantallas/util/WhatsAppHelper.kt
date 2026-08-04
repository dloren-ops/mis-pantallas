package com.dloren.mispantallas.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades para enviar y recibir datos de cuentas mediante WhatsApp.
 *
 * El mensaje usa un formato "clave: valor" por línea, lo que permite tanto que
 * sea legible para el cliente como que la app pueda volver a parsearlo cuando se
 * comparte de vuelta hacia la app.
 */
object WhatsAppHelper {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Etiquetas usadas en el mensaje (deben coincidir con el parser).
    private const val L_PLATFORM = "Plataforma"
    private const val L_EMAIL = "Correo"
    private const val L_PASSWORD = "Contraseña"
    private const val L_PROFILE = "Perfil"
    private const val L_PIN = "PIN"
    private const val L_DURATION = "Duración (días)"
    private const val L_START = "Inicio"
    private const val L_END = "Vence"

    /**
     * Construye el mensaje de texto con los datos de la cuenta. [customHeader] y
     * [customFooter] son el encabezado y el mensaje final configurados en Ajustes;
     * si vienen vacíos se usa el mensaje predeterminado (encabezado) o directamente
     * no se agrega nada (mensaje final).
     */
    fun buildMessage(
        context: Context,
        account: Account,
        customHeader: String = "",
        customFooter: String = ""
    ): String {
        val header = customHeader.ifBlank { context.getString(R.string.default_whatsapp_message) }
        val sb = StringBuilder()
        sb.appendLine(header)
        sb.appendLine()
        if (account.platform.isNotBlank()) sb.appendLine("$L_PLATFORM: ${account.platform}")
        if (account.email.isNotBlank()) sb.appendLine("$L_EMAIL: ${account.email}")
        if (account.password.isNotBlank()) sb.appendLine("$L_PASSWORD: ${account.password}")
        if (account.profileName.isNotBlank()) sb.appendLine("$L_PROFILE: ${account.profileName}")
        if (account.pin.isNotBlank()) sb.appendLine("$L_PIN: ${account.pin}")
        sb.appendLine("$L_DURATION: ${account.durationDays}")
        sb.appendLine("$L_START: ${dateFormat.format(Date(account.startDateMillis))}")
        sb.appendLine("$L_END: ${dateFormat.format(Date(account.endDateMillis))}")
        if (customFooter.isNotBlank()) {
            sb.appendLine()
            sb.appendLine(customFooter)
        }
        return sb.toString().trim()
    }

    /**
     * Construye el mensaje de saludo + pregunta de renovación para un cliente cuya
     * pantalla está por vencer (o ya venció). [customMessage] es la plantilla
     * configurada en Ajustes (admite los tokens {plataforma} y {fecha}); si viene
     * vacía se usa la plantilla predeterminada.
     */
    fun buildRenewalReminderMessage(
        context: Context,
        account: Account,
        customMessage: String = ""
    ): String {
        val template = customMessage.ifBlank { context.getString(R.string.default_renewal_reminder_message) }
        val platformPhrase = if (account.platform.isNotBlank()) {
            context.getString(R.string.renewal_reminder_platform_phrase, account.platform)
        } else {
            context.getString(R.string.renewal_reminder_platform_phrase_generic)
        }
        val dateStr = dateFormat.format(Date(account.endDateMillis))
        return template
            .replace("{plataforma}", platformPhrase)
            .replace("{fecha}", dateStr)
    }

    /** Abre WhatsApp con el mensaje de recordatorio de renovación para el cliente de [account]. */
    fun sendRenewalReminder(
        context: Context,
        account: Account,
        customMessage: String = ""
    ) {
        sendText(context, account.clientPhone, buildRenewalReminderMessage(context, account, customMessage))
    }

    /**
     * Abre WhatsApp con el mensaje prellenado. Si la cuenta tiene teléfono del
     * cliente, abre el chat directo; si no, abre el selector de contacto.
     */
    fun sendToWhatsApp(
        context: Context,
        account: Account,
        customHeader: String = "",
        customFooter: String = ""
    ) {
        sendText(context, account.clientPhone, buildMessage(context, account, customHeader, customFooter))
    }

    /** Abre WhatsApp con un texto arbitrario, sin depender de una [Account]. */
    fun sendText(context: Context, phone: String, message: String) {
        val cleanPhone = phone.filter { it.isDigit() }

        try {
            if (cleanPhone.isNotBlank()) {
                // API oficial de click-to-chat de WhatsApp: abre el chat directo
                // (funciona con WhatsApp normal y con Business, según cuál tenga
                // el número registrado o cuál elija el sistema si hay ambas).
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
                    )
                )
            } else {
                // Sin número (se usó un alias/@usuario): no se puede armar un link
                // de chat directo, así que se deja que el usuario elija la app y el
                // contacto. No se fuerza el paquete "com.whatsapp" porque eso
                // ignora WhatsApp Business si ambas apps están instaladas.
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(send, null))
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                R.string.whatsapp_not_installed,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Intenta extraer los datos de una cuenta desde un texto compartido.
     * Reconoce el formato "etiqueta: valor" generado por [buildMessage] y también
     * variantes comunes (email, contraseña, etc.). Devuelve null si no logra
     * identificar ningún campo.
     */
    fun parseSharedText(text: String?): Account? {
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
