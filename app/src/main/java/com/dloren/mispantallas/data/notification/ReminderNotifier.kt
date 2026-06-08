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
 * Crea el canal de notificaciones y muestra el recordatorio de vencimiento.
 * Al tocar la notificación se abre WhatsApp con el mensaje de renovación ya
 * escrito para el cliente (si tiene número cargado).
 */
object ReminderNotifier {

    private const val CHANNEL_ID = "renewal_reminders"

    fun show(context: Context, account: Account) {
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
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent()
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            account.id.toInt(),
            tapIntent,
            flags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text, label))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(account.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones (Android 13+); se ignora silenciosamente.
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminder_channel_desc)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
