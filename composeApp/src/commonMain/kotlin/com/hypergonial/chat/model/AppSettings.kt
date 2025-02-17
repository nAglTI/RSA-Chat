package com.hypergonial.chat.model

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

    /** The JSON serializer/deserializer */
    private var json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Get a serializable object from the settings
     *
     * @param key The key to get the object for
     * @return The object or null if it does not exist
     */
    private inline fun <reified T> getSerializable(key: String): @Serializable T? {
        val value = userPreferences.getStringOrNull(key)

        return if (value.isNullOrEmpty()) null else json.decodeFromString(value)
    }

    /**
     * Set a serializable object in the settings
     *
     * @param key The key to set the object for
     * @param value The object to set
     */
    private inline fun <reified T> setSerializable(key: String, value: @Serializable T) {
        val serialized = json.encodeToString(value)

        userPreferences.putString(key, serialized)
    }

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

    /**
     * Get the developer settings
     *
     * @return The developer settings or the default settings if none are set
     */
    fun getDevSettings(): DevSettings {
        if (cachedDevSettings != null) {
            return cachedDevSettings!!
        } else {
            val config = getSerializable("API_CONFIG") ?: DevSettings.default()
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
        setSerializable("API_CONFIG", config)
        cachedDevSettings = config
    }

    fun getLastOpenedPrefs(): LastOpenedPrefs {
        return getSerializable("LAST_OPENED_PREFS") ?: LastOpenedPrefs.default()
    }

    fun setLastOpenedPrefs(prefs: LastOpenedPrefs) {
        setSerializable("LAST_OPENED_PREFS", prefs)
    }
}

/** The persistent settings store for this platform */
expect val settings: AppSettings
