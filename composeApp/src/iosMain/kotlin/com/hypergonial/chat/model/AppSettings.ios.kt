package com.hypergonial.chat.model

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings


@OptIn(ExperimentalSettingsImplementation::class)
class IOSAppSettings: AppSettings {
    override val userPreferences = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    override val secrets = KeychainSettings("com.hypergonial.chat.secrets")
}

actual val settings: AppSettings = IOSAppSettings()
