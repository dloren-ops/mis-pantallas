package com.dloren.mispantallas.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dloren.mispantallas.R
import com.dloren.mispantallas.domain.model.Account

/**
 * Crea los canales y muestra los recordatorios de vencimiento:
 *  - Cliente: al tocar abre WhatsApp con el mensaje de renovación para el cliente.
 *  - Proveedor: te avisa que vos debés renovar la cuenta con el proveedor.
 */
object ReminderNotifier {

    private const val CHANNEL_ID = "renewal_reminders"

    /** Recordatorio para avisar al cliente que renueve su alquiler. */
    fun showClientReminder(context: Context, account: Account) {
        createChannel(context)

        val label = account.platform.ifBlank { account.email.ifBlank { "Cuenta" } }
        val platformSuffix = if (account.platform.isNotBlank()) " de ${account.platform}" else ""
        val waText = context.getString(R.string.renewal_whatsapp_message, platformSuffix)
        val phone = account.clientPhone.filter { it.isDigit() }

        val tapIntent = if (phone.isNotBlank()) {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/$phone?text=${Uri.encode(waText)}")
            )
        } else {
            launchIntent(context)
        }

        notify(
            context = context,
            id = account.id.toInt(),
            title = context.getString(R.string.reminder_title),
            text = context.getString(R.string.reminder_text, label),
            intent = tapIntent
        )
    }

    /** Recordatorio propio: te avisa que tenés que renovar con el proveedor. */
    fun showProviderReminder(context: Context, account: Account) {
        createChannel(context)
        val label = account.platform.ifBlank { account.email.ifBlank { "tu cuenta" } }
        notify(
            context = context,
            // Distinto id para no pisar la notificación del cliente.
            id = ("p" + account.id).hashCode(),
            title = context.getString(R.string.provider_reminder_title),
            text = context.getString(R.string.provider_reminder_text, label),
            intent = launchIntent(context)
        )
    }

    private fun notify(context: Context, id: Int, title: String, text: String, intent: Intent) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, id, intent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones (Android 13+); se ignora.
        }
    }

    private fun launchIntent(context: Context): Intent =
        context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminder_channel_desc)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
}
