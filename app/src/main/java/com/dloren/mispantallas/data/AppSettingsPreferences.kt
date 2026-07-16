package com.dloren.mispantallas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings_prefs")

/** Días de anticipación por defecto para el recordatorio de vencimiento. */
const val DEFAULT_REMINDER_DAYS = 3

/** Mensaje personalizado de WhatsApp y días de anticipación del recordatorio de vencimiento. */
class AppSettingsPreferences(private val context: Context) {

    private val whatsappMessageKey = stringPreferencesKey("whatsapp_message")
    private val whatsappFooterKey = stringPreferencesKey("whatsapp_footer")
    private val renewalReminderMessageKey = stringPreferencesKey("renewal_reminder_message")
    private val reminderDaysKey = intPreferencesKey("reminder_days")

    /** Vacío significa "usar el mensaje predeterminado". */
    val whatsappMessage: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[whatsappMessageKey] ?: ""
    }

    /** Vacío significa "sin mensaje final". */
    val whatsappFooter: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[whatsappFooterKey] ?: ""
    }

    /** Vacío significa "usar el mensaje predeterminado". */
    val renewalReminderMessage: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[renewalReminderMessageKey] ?: ""
    }

    val reminderDays: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        prefs[reminderDaysKey] ?: DEFAULT_REMINDER_DAYS
    }

    suspend fun saveWhatsappMessage(message: String) {
        context.appSettingsDataStore.edit { prefs -> prefs[whatsappMessageKey] = message }
    }

    suspend fun saveWhatsappFooter(footer: String) {
        context.appSettingsDataStore.edit { prefs -> prefs[whatsappFooterKey] = footer }
    }

    suspend fun saveRenewalReminderMessage(message: String) {
        context.appSettingsDataStore.edit { prefs -> prefs[renewalReminderMessageKey] = message }
    }

    suspend fun saveReminderDays(days: Int) {
        context.appSettingsDataStore.edit { prefs -> prefs[reminderDaysKey] = days.coerceAtLeast(1) }
    }
}
