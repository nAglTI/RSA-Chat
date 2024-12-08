package com.hypergonial.chat.model

import com.russhwolf.settings.Settings

interface AppSettings {
    val userPreferences: Settings
    val secrets: Settings?

    private fun getSecret(key: String): String? {
        val value = if (secrets == null) {
            userPreferences.getStringOrNull(key)
        }
        else {
            secrets!!.getStringOrNull(key)
        }

        return if (value.isNullOrEmpty()) null else value
    }

    private fun setSecret(key: String, value: String) {
        if (secrets == null) {
            userPreferences.putString(key, value)
        } else {
            secrets!!.putString(key, value)
        }
    }

    fun getToken(): String? = getSecret("TOKEN")

    fun setToken(token: String) = setSecret("TOKEN", token)

    fun removeToken() = setSecret("TOKEN", "")
}


expect val settings: AppSettings
