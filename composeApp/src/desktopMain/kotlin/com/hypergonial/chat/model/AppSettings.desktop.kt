package com.hypergonial.chat.model

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

class DesktopSettings : AppSettings {
    override val userPreferences: Settings = PreferencesSettings(Preferences.userRoot().node("com.hypergonial.chat"))
    override val secrets: Settings? = null
}


actual val settings: AppSettings = DesktopSettings()
