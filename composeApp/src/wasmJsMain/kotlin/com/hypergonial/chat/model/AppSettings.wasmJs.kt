package com.hypergonial.chat.model

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.localStorage

class LocalStorageSettings : AppSettings() {
    override val userPreferences: Settings = StorageSettings(localStorage)
    override val secrets: Settings? = null
}

actual val settings: AppSettings = LocalStorageSettings()
