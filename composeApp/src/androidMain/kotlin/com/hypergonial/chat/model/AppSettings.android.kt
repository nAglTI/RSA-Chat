package com.hypergonial.chat.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceDataStore
import com.hypergonial.chat.data.SecuredDataStore
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings

@OptIn(ExperimentalSettingsApi::class)
class AndroidSettings : AppSettings() {
    override var userPreferences: Settings
        get() {
            if (potentiallyPendingUserPrefs != null) {
                return potentiallyPendingUserPrefs!!
            } else {
                error("User preferences not initialized")
            }
        }
        set(value) {
            potentiallyPendingUserPrefs = value
        }

    override val secrets: Settings? = null // TODO: secured prefs

    override var androidSecrets: FlowSettings
        get() {
            if (potentiallyPendingSecrets != null) {
                return potentiallyPendingSecrets!!
            } else {
                error("User preferences not initialized")
            }
        }
        set(value) {
            potentiallyPendingSecrets = value
        }

    private var potentiallyPendingUserPrefs: Settings? = null
    private var potentiallyPendingSecrets: FlowSettings? = null

    fun initialize(userPreferences: SharedPreferences, securedDatastore: SecuredDataStore) {
        this.userPreferences = SharedPreferencesSettings(userPreferences)
        this.androidSecrets = SecuredSettings(securedDatastore)
    }
}

actual val settings: AppSettings = AndroidSettings()
