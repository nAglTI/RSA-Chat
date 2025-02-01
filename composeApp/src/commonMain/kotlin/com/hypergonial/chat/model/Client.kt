package com.hypergonial.chat.model

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.coroutines.CoroutineScope

interface Client : InstanceKeeper.Instance, EventManagerAware, CacheAware {
    /** The coroutine scope of the client */
    val scope: CoroutineScope

    /** If true, the client is paused and will not perform background tasks */
    val isSuspended: Boolean

    /** A random session ID for the client to use
     * This is generally used to differentiate between sessions of the same user on different devices.
     */
    val sessionId: String

    /** Replace the coroutine scope of the client with a different one.
     *
     * Stops and restarts all background tasks on the new scope.
     * */
    fun replaceScope(scope: CoroutineScope)

    fun pause()

    suspend fun resume()

    /** Check if the client is logged in
     *
     * It returns true if the client has an authorization token stored.
     */
    fun isLoggedIn(): Boolean

    /** Try logging in with the provided credentials
     *
     * @param username The username to log in with
     * @param password The password to log in with
     *
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the login fails
     *
     * If successful, the client will store the authorization token and subsequent requests will be authenticated.
     */
    suspend fun login(username: String, password: Secret<String>)

    /** Register a new user with the provided credentials
     *
     * @param username The username to register with
     * @param password The password to register with
     */
    suspend fun register(username: String, password: Secret<String>)

    /** Connects to the gateway
     *
     * This function returns immediately, and the client will connect in the background.
     */
    suspend fun connect()

    /** Check if the client is connected to the gateway */
    fun isConnected(): Boolean

    /** Wait until the client is connected to the gateway */
    suspend fun waitUntilConnected()

    /** Wait until the client is disconnected from the gateway */
    suspend fun waitUntilDisconnected()


    /** Fetch a batch of messages from the given channel.
     *
     * @param channelId The ID of the channel to fetch messages from
     * @param before Fetch messages before this message ID
     * @param after Fetch messages after this message ID
     * @param limit The maximum number of messages to fetch
     *
     * @return A list of messages fetched from the channel, sorted by creation date in ascending order.
     *
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the channel does not exist
     */
    suspend fun fetchMessages(
        channelId: Snowflake,
        before: Snowflake? = null,
        after: Snowflake? = null,
        limit: UInt = 100u
    ): List<Message>

    /** Send a message to the given channel */
    suspend fun sendMessage(channelId: Snowflake, content: String, nonce: String? = null): Message

    /** Log out the currently authenticated user
     *
     * Wipes all client state.
     * */
    fun logout()

    /** Closes the gateway connection, if it is open. */
    fun closeGateway()

    /** Edit a message by its ID */
    suspend fun editMessage(channelId: Snowflake, messageId: Snowflake, content: String? = null): Message

    /** Delete a message by its ID */
    suspend fun deleteMessage(channelId: Snowflake, messageId: Snowflake)

    /** Reloads the client API configuration from disk */
    fun reloadConfig()

    /** Wait until the client is ready */
    suspend fun waitUntilReady()

    /** Check if the client is ready */
    fun isReady(): Boolean

    /** Fetch a guild by its ID */
    suspend fun fetchGuild(guildId: Snowflake): Guild

    /** Fetch a channel by its ID */
    suspend fun fetchChannel(channelId: Snowflake): Channel

    /** Join a new guild as the currently authenticated user */
    suspend fun joinGuild(guildId: Snowflake): Member

    /** Creates a new guild with the given name */
    suspend fun createGuild(name: String): Guild

    /** Creates a new channel in the given guild */
    suspend fun createChannel(guildId: Snowflake, name: String): Channel

    /** Check if the given username is available */
    suspend fun checkUsernameForAvailability(username: String): Boolean
}
