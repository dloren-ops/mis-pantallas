package com.dloren.mispantallas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.licenseDataStore by preferencesDataStore(name = "license_prefs")

/** Caché local de la licencia del cliente, para arrancar sin depender de la red. */
class LicensePreferences(private val context: Context) {

    private object Keys {
        val CODE = stringPreferencesKey("code")
        val EXPIRES_AT = longPreferencesKey("expires_at")
    }

    val cachedCode: Flow<String?> = context.licenseDataStore.data.map { it[Keys.CODE] }
    val cachedExpiresAt: Flow<Long?> = context.licenseDataStore.data.map { it[Keys.EXPIRES_AT] }

    suspend fun readCachedCode(): String? = cachedCode.first()
    suspend fun readCachedExpiresAt(): Long? = cachedExpiresAt.first()

    suspend fun saveLicense(code: String, expiresAtMillis: Long) {
        context.licenseDataStore.edit {
            it[Keys.CODE] = code
            it[Keys.EXPIRES_AT] = expiresAtMillis
        }
    }

    suspend fun clear() {
        context.licenseDataStore.edit { it.clear() }
    }
}
