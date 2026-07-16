package com.dloren.mispantallas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDefaultsDataStore by preferencesDataStore(name = "profile_defaults_prefs")

/** Cuántos perfiles como máximo se pueden generar de una vez en "Crear perfiles". */
const val MAX_PROFILE_SLOTS = 7

/** Nombre y PIN por defecto de cada perfil (1..[MAX_PROFILE_SLOTS]) para "Crear perfiles". */
class ProfileDefaultsPreferences(private val context: Context) {

    private val nameKeys = (1..MAX_PROFILE_SLOTS).map { stringPreferencesKey("profile_name_$it") }
    private val pinKeys = (1..MAX_PROFILE_SLOTS).map { stringPreferencesKey("profile_pin_$it") }

    val defaultNames: Flow<List<String>> = context.profileDefaultsDataStore.data.map { prefs ->
        nameKeys.map { key -> prefs[key] ?: "" }
    }

    val defaultPins: Flow<List<String>> = context.profileDefaultsDataStore.data.map { prefs ->
        pinKeys.map { key -> prefs[key] ?: "" }
    }

    suspend fun saveNames(names: List<String>) {
        context.profileDefaultsDataStore.edit { prefs ->
            nameKeys.forEachIndexed { index, key -> prefs[key] = names.getOrElse(index) { "" } }
        }
    }

    suspend fun savePins(pins: List<String>) {
        context.profileDefaultsDataStore.edit { prefs ->
            pinKeys.forEachIndexed { index, key -> prefs[key] = pins.getOrElse(index) { "" } }
        }
    }
}
