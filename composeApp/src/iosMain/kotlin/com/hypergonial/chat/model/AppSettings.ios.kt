package com.hypergonial.chat.model

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.coroutines.FlowSettings
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalSettingsImplementation::class)
class IOSAppSettings : AppSettings() {
    override val userPreferences = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    override val secrets = KeychainSettings("com.hypergonial.chat.secrets")

    @OptIn(ExperimentalSettingsApi::class)
    override val androidSecrets: FlowSettings? = null
}

actual val settings: AppSettings = IOSAppSettings()
