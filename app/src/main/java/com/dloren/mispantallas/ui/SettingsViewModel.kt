package com.dloren.mispantallas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.AppSettingsPreferences
import com.dloren.mispantallas.data.DEFAULT_REMINDER_DAYS
import com.dloren.mispantallas.data.MAX_PROFILE_SLOTS
import com.dloren.mispantallas.data.ProfileDefaultsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = ProfileDefaultsPreferences(app)
    private val appSettingsPrefs = AppSettingsPreferences(app)

    val defaultNames: StateFlow<List<String>> = prefs.defaultNames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = List(MAX_PROFILE_SLOTS) { "" }
        )

    val defaultPins: StateFlow<List<String>> = prefs.defaultPins
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = List(MAX_PROFILE_SLOTS) { "" }
        )

    val whatsappMessage: StateFlow<String> = appSettingsPrefs.whatsappMessage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    val whatsappFooter: StateFlow<String> = appSettingsPrefs.whatsappFooter
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    val renewalReminderMessage: StateFlow<String> = appSettingsPrefs.renewalReminderMessage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    val reminderDays: StateFlow<Int> = appSettingsPrefs.reminderDays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DEFAULT_REMINDER_DAYS
        )

    fun saveNames(names: List<String>) {
        viewModelScope.launch { prefs.saveNames(names) }
    }

    fun savePins(pins: List<String>) {
        viewModelScope.launch { prefs.savePins(pins) }
    }

    fun saveWhatsappMessage(message: String) {
        viewModelScope.launch { appSettingsPrefs.saveWhatsappMessage(message) }
    }

    fun saveWhatsappFooter(footer: String) {
        viewModelScope.launch { appSettingsPrefs.saveWhatsappFooter(footer) }
    }

    fun saveRenewalReminderMessage(message: String) {
        viewModelScope.launch { appSettingsPrefs.saveRenewalReminderMessage(message) }
    }

    fun saveReminderDays(days: Int) {
        viewModelScope.launch { appSettingsPrefs.saveReminderDays(days) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as Application
                return SettingsViewModel(app) as T
            }
        }
    }
}
