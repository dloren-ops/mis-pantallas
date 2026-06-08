package com.dloren.mispantallas.presentation.whatsapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dloren.mispantallas.R
import com.dloren.mispantallas.domain.model.Account
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Construye el mensaje de datos de una cuenta y lo envía por WhatsApp.
 * Es código dependiente del framework de Android (intents), por eso vive en la
 * capa de presentación.
 */
object WhatsAppLauncher {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private const val L_PLATFORM = "Plataforma"
    private const val L_EMAIL = "Correo"
    private const val L_PASSWORD = "Contraseña"
    private const val L_PROFILE = "Perfil"
    private const val L_PIN = "PIN"
    private const val L_DURATION = "Duración (días)"
    private const val L_START = "Inicio"
    private const val L_END = "Vence"

    /** Construye el texto con los datos de la cuenta (formato "clave: valor"). */
    fun buildMessage(context: Context, account: Account): String {
        val sb = StringBuilder()
        sb.appendLine(context.getString(R.string.default_whatsapp_message))
        sb.appendLine()
        if (account.platform.isNotBlank()) sb.appendLine("$L_PLATFORM: ${account.platform}")
        if (account.email.isNotBlank()) sb.appendLine("$L_EMAIL: ${account.email}")
        if (account.password.isNotBlank()) sb.appendLine("$L_PASSWORD: ${account.password}")
        if (account.profileName.isNotBlank()) sb.appendLine("$L_PROFILE: ${account.profileName}")
        if (account.pin.isNotBlank()) sb.appendLine("$L_PIN: ${account.pin}")
        sb.appendLine("$L_DURATION: ${account.durationDays}")
        sb.appendLine("$L_START: ${dateFormat.format(Date(account.startDateMillis))}")
        sb.appendLine("$L_END: ${dateFormat.format(Date(account.endDateMillis))}")
        return sb.toString().trim()
    }

    /** Abre WhatsApp con el mensaje prellenado (chat directo si hay teléfono). */
    fun send(context: Context, account: Account) {
        val message = buildMessage(context, account)
        val phone = account.clientPhone.filter { it.isDigit() }
        try {
            val intent = if (phone.isNotBlank()) {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
                )
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, message)
                }
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(fallback, null))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show()
            }
        }
    }
}
