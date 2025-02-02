package com.hypergonial.chat.model

import android.content.SharedPreferences
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

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

    private var potentiallyPendingUserPrefs: Settings? = null

    fun initialize(userPreferences: SharedPreferences) {
        this.userPreferences = SharedPreferencesSettings(userPreferences)
    }
}

actual val settings: AppSettings = AndroidSettings()
