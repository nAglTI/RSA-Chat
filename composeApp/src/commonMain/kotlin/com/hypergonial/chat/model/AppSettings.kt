package com.hypergonial.chat.model

import com.hypergonial.chat.SettingsExt.getSerializable
import com.hypergonial.chat.SettingsExt.setSerializable
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.toSnowflake
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/** The base class for implementing a persistent settings store for the application */
@OptIn(ExperimentalSettingsApi::class)
abstract class AppSettings : SynchronizedObject() {
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

    protected abstract val androidSecrets: FlowSettings?

    private var _cachedDevSettings: DevSettings? = null

    /** The cached developer settings for faster access */
    private var cachedDevSettings: DevSettings?
        get() = synchronized(this) { _cachedDevSettings }
        set(value) = synchronized(this) { _cachedDevSettings = value }

    /**
     * Get a secret from the settings, uses the secrets store if one is available on the platform.
     *
     * @param key The key to get the secret for
     * @return The secret or null if it does not exist
     */
    private suspend fun getSecret(key: String): String? {
        val value = secrets?.getStringOrNull(key)
            ?: androidSecrets?.getStringOrNull(key)
            ?: userPreferences.getStringOrNull(key)

        return if (value.isNullOrEmpty()) null else value
    }

    /**
     * Set a secret in the settings, uses the secrets store if one is available on the platform.
     *
     * @param key The key to set the secret for
     * @param value The secret to set
     */
    private suspend fun setSecret(key: String, value: String) { // TODO: use android secrets
        secrets?.putString(key, value)
            ?: androidSecrets?.putString(key, value)
            ?: userPreferences.putString(key, value)
    }

    /** Clear all settings */
    fun clear() {
        userPreferences.clear()
        secrets?.clear()
        TODO("android suspended clear()")
    }

    /** Clear all settings specific to the current user */
    fun clearUserPreferences() {
        clearLastOpenedPrefs()
    }

    // TODO: mb impl local coroutine scope + job with IO
    /**
     * Get the current user's token
     *
     * @return The token or null if no one is currently authenticated
     */
    suspend fun getToken(): String? = withContext(Dispatchers.IO) { getSecret("TOKEN") }

    /**
     * Set the current user's token
     *
     * @param token The token to set
     */
    suspend fun setToken(token: String) = withContext(Dispatchers.IO) { setSecret("TOKEN", token) }

    /** Remove the current user's token, effectively logging them out */
    suspend fun removeToken() = withContext(Dispatchers.IO) { setSecret("TOKEN", "") }

    /** Get the last time the token was refreshed */
    fun getLastTokenRefresh(): Instant = userPreferences.getSerializable("TOKEN_REFRESH") ?: Instant.DISTANT_PAST

    /** Set the last time the token was refreshed */
    fun setLastTokenRefresh(time: Instant) = userPreferences.setSerializable("TOKEN_REFRESH", time)

    /** Get the current user's FCM token */
    fun getFCMSettings(): FCMSettings? = userPreferences.getSerializable("FCM_SETTINGS")

    /** Set the current user's FCM token */
    fun setFCMSettings(settings: FCMSettings) = userPreferences.setSerializable("FCM_SETTINGS", settings)

    /** Remove the current user's FCM token */
    fun clearFCMSettings() = userPreferences.remove("FCM_SETTINGS")

    /** Get all currently active notifications */
    private fun getNotifications(): HashMap<Snowflake, HashSet<Int>> {
        synchronized(this) {
            val notifs = userPreferences.getSerializable<HashMap<Snowflake, HashSet<Int>>>("NOTIFICATIONS")
            return notifs ?: HashMap()
        }
    }

    /** Set all currently active notifications */
    private fun setNotifications(notifs: HashMap<Snowflake, HashSet<Int>>) {
        synchronized(this) { userPreferences.setSerializable("NOTIFICATIONS", notifs) }
    }

    /** Add a new notification to the list of active notifications. */
    fun pushNotification(channelId: Snowflake, notificationId: Int) {
        synchronized(this) {
            val notifs = getNotifications()
            notifs.getOrPut(channelId) { HashSet() }.add(notificationId)
            setNotifications(notifs)
        }
    }

    /** Remove a notification from the list of active notifications. */
    fun popNotification(channelId: Snowflake, notificationId: Int) {
        synchronized(this) {
            val notifs = getNotifications()
            notifs[channelId]?.remove(notificationId)
            setNotifications(notifs)
        }
    }

    /** Get all notifications for a specific channel */
    fun getNotificationsIn(channelId: Snowflake): Set<Int>? {
        synchronized(this) {
            val notifs = getNotifications()
            return notifs[channelId]
        }
    }

    /** Clear all notifications for a specific channel */
    fun clearNotificationsIn(channelId: Snowflake) {
        synchronized(this) {
            val notifs = getNotifications()
            notifs.remove(channelId)
            setNotifications(notifs)
        }
    }

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

    /**
     * Get the last opened guilds and channels
     *
     * @return The last opened guilds and channels by this user
     */
    fun getLastOpenedPrefs(): LastOpenedPrefs {
        return userPreferences.getSerializable("LAST_OPENED_PREFS") ?: LastOpenedPrefs.default()
    }

    /**
     * Set the last opened guilds and channels
     *
     * @param prefs The last opened guilds and channels by this user
     */
    fun setLastOpenedPrefs(prefs: LastOpenedPrefs) {
        userPreferences.setSerializable("LAST_OPENED_PREFS", prefs)
    }

    /** Clear the last opened guilds and channels */
    fun clearLastOpenedPrefs() {
        userPreferences.remove("LAST_OPENED_PREFS")
    }

    suspend fun setPrivateKey(key: String) = withContext(Dispatchers.IO) {
        setSecret(SECRET_KEY_NAME, key)
    }

    suspend fun getPrivateKey(): String? = withContext(Dispatchers.IO) { getSecret(SECRET_KEY_NAME) }

    private companion object {
        private const val SECRET_KEY_NAME = "pk_set_secured_pref"
    }
}

// TODO: mb there is other way to handle property
/** The persistent settings store for this platform */
expect val settings: AppSettings
