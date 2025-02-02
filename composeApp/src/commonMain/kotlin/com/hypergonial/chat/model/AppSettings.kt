package com.hypergonial.chat.model

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

abstract class AppSettings {
    /** The user preferences store
     *
     * It should be used to store insensitive user data, such as API settings. */
    protected abstract val userPreferences: Settings
    /** The secrets store
     *
     * It should be used to store sensitive user data, such as tokens.
     *
     * Note that not all platforms implement this, in that case, the user preferences store should be used. */
    protected abstract val secrets: Settings?

    /** The cached API settings for faster access */
    private var cachedApiConfig: ApiConfig? = null

    /** Get a serializable object from the settings
     *
     * @param key The key to get the object for
     * @return The object or null if it does not exist
     * */
    private inline fun <reified T> getSerializable(key: String): @Serializable T? {
        val value = userPreferences.getStringOrNull(key)

        return if (value.isNullOrEmpty()) null else Json.decodeFromString(value)
    }

    /** Set a serializable object in the settings
     *
     * @param key The key to set the object for
     * @param value The object to set
     * */
    private inline fun <reified T> setSerializable(key: String, value: @Serializable T) {
        val serialized = Json.encodeToString(value)

        userPreferences.putString(key, serialized)
    }

    /** Get a secret from the settings, uses the secrets store if one is available on the platform.
     *
     * @param key The key to get the secret for
     * @return The secret or null if it does not exist
     * */
    private fun getSecret(key: String): String? {
        val value = if (secrets == null) {
            userPreferences.getStringOrNull(key)
        }
        else {
            secrets!!.getStringOrNull(key)
        }

        return if (value.isNullOrEmpty()) null else value
    }

    /** Set a secret in the settings, uses the secrets store if one is available on the platform.
     *
     * @param key The key to set the secret for
     * @param value The secret to set
     * */
    private fun setSecret(key: String, value: String) {
        if (secrets == null) {
            userPreferences.putString(key, value)
        } else {
            secrets!!.putString(key, value)
        }
    }

    /** Get the current user's token
     *
     * @return The token or null if no one is currently authenticated
     * */
    fun getToken(): String? = getSecret("TOKEN")

    /** Set the current user's token
     *
     * @param token The token to set
     * */
    fun setToken(token: String) = setSecret("TOKEN", token)

    /** Remove the current user's token, effectively logging them out
     */
    fun removeToken() = setSecret("TOKEN", "")

    /** Get the API settings
     *
     * @return The API settings or the default settings if none are set
     * */
    fun getApiSettings(): ApiConfig {
        if (cachedApiConfig != null) {
            return cachedApiConfig!!
        }
        else {
            val config = getSerializable("API_CONFIG") ?: ApiConfig.default()
            cachedApiConfig = config
            return config
        }
    }

    /** Set the API settings
     *
     * @param config The API settings to set
     * */
    fun setApiSettings(config: ApiConfig) {
        setSerializable("API_CONFIG", config)
        cachedApiConfig = config
    }

}

/** The persistent settings store for this platform */
expect val settings: AppSettings
