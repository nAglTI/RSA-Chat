package com.hypergonial.chat.model

import co.touchlab.kermit.Logger
import com.hypergonial.chat.SettingsExt.getSerializable
import com.hypergonial.chat.SettingsExt.setSerializable
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.toSnowflake
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The base class for implementing a persistent settings store for the application */
abstract class AppSettings {
    /**
     * The user preferences store
     *
     * It should be used to store insensitive user data, such as API settings.
     */
    protected abstract val userPreferences: Settings
    /**
     * The secrets store
     *
     * It should be used to store sensitive user data, such as tokens.
     *
     * Note that not all platforms implement this, in that case, the user preferences store should be used.
     */
    protected abstract val secrets: Settings?

    /** The cached developer settings for faster access */
    private var cachedDevSettings: DevSettings? = null

    /**
     * Get a secret from the settings, uses the secrets store if one is available on the platform.
     *
     * @param key The key to get the secret for
     * @return The secret or null if it does not exist
     */
    private fun getSecret(key: String): String? {
        val value =
            if (secrets == null) {
                userPreferences.getStringOrNull(key)
            } else {
                secrets!!.getStringOrNull(key)
            }

        return if (value.isNullOrEmpty()) null else value
    }

    /**
     * Set a secret in the settings, uses the secrets store if one is available on the platform.
     *
     * @param key The key to set the secret for
     * @param value The secret to set
     */
    private fun setSecret(key: String, value: String) {
        if (secrets == null) {
            userPreferences.putString(key, value)
        } else {
            secrets!!.putString(key, value)
        }
    }

    /** Clear all settings */
    fun clear() {
        userPreferences.clear()
        secrets?.clear()
    }

    /** Clear all settings specific to the current user */
    fun clearUserPreferences() {
        clearLastOpenedPrefs()
    }

    /**
     * Get the current user's token
     *
     * @return The token or null if no one is currently authenticated
     */
    fun getToken(): String? = getSecret("TOKEN")

    /**
     * Set the current user's token
     *
     * @param token The token to set
     */
    fun setToken(token: String) = setSecret("TOKEN", token)

    /** Remove the current user's token, effectively logging them out */
    fun removeToken() = setSecret("TOKEN", "")

    /** Get the last user's ID we logged in as */
    fun getLastLoggedInAs(): Snowflake? {
        val value = userPreferences.getStringOrNull("LAST_LOGGED_IN_ID")
        return if (value.isNullOrEmpty()) null else value.toSnowflake()
    }

    /** Set the last user's ID we logged in as */
    fun setLastLoggedInAs(id: Snowflake) {
        userPreferences.putString("LAST_LOGGED_IN_ID", id.toString())
    }

    /**
     * Get the developer settings
     *
     * @return The developer settings or the default settings if none are set
     */
    fun getDevSettings(): DevSettings {
        if (cachedDevSettings != null) {
            return cachedDevSettings!!
        } else {
            val config = userPreferences.getSerializable("API_CONFIG") ?: DevSettings.default()
            cachedDevSettings = config
            return config
        }
    }

    /**
     * Set the Developer settings
     *
     * @param config The developer settings to set
     */
    fun setDevSettings(config: DevSettings) {
        userPreferences.setSerializable("API_CONFIG", config)
        cachedDevSettings = config
    }

    /** Get the last opened guilds and channels
     *
     * @return The last opened guilds and channels by this user
     * */
    fun getLastOpenedPrefs(): LastOpenedPrefs {
        return userPreferences.getSerializable("LAST_OPENED_PREFS") ?: LastOpenedPrefs.default()
    }

    /** Set the last opened guilds and channels
     *
     * @param prefs The last opened guilds and channels by this user
     * */
    fun setLastOpenedPrefs(prefs: LastOpenedPrefs) {
        userPreferences.setSerializable("LAST_OPENED_PREFS", prefs)
    }

    /** Clear the last opened guilds and channels */
    fun clearLastOpenedPrefs() {
        userPreferences.remove("LAST_OPENED_PREFS")
    }
}

/** The persistent settings store for this platform */
expect val settings: AppSettings
