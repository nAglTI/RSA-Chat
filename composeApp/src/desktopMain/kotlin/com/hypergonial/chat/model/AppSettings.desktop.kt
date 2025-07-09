package com.hypergonial.chat.model

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import java.util.prefs.Preferences

class DesktopSettings : AppSettings() {
    override val userPreferences: Settings = PreferencesSettings(Preferences.userRoot().node("com.hypergonial.chat"))
    override val secrets: Settings? = null

    @OptIn(ExperimentalSettingsApi::class)
    override val androidSecrets: FlowSettings? = null
}

actual val settings: AppSettings = DesktopSettings()
