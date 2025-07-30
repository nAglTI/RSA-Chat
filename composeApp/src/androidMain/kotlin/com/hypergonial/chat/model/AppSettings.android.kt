package com.hypergonial.chat.model

import android.content.SharedPreferences
import com.hypergonial.chat.data.SecuredDataStore
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings

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

    override val secrets: Settings? = null

    override var androidSecrets: FlowSettings
        get() {
            if (potentiallyPendingSecrets != null) {
                return potentiallyPendingSecrets!!
            } else {
                error("Android secrets not initialized")
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
