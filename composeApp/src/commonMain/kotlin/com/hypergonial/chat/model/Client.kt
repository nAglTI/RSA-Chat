package com.hypergonial.chat.model

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.coroutines.CoroutineScope

interface Client : InstanceKeeper.Instance, EventManagerAware, CacheAware {

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


    /** Fetch a batch of messages from the given channel.
     *
     * @param channelId The ID of the channel to fetch messages from
     */
    suspend fun fetchMessages(
        channelId: Snowflake,
        before: Snowflake? = null,
        after: Snowflake? = null,
        limit: UInt = 100u
    ): List<Message>

    /** Send a message to the given channel */
    suspend fun sendMessage(channelId: Snowflake, content: String, nonce: String? = null)

    /** Logout the currently authenticated user */
    fun logout()

    /** Closes the gateway connection, if it is open. */
    fun closeGateway()

    /** Edit a message by its ID */
    suspend fun editMessage(channelId: Snowflake, messageId: Snowflake, content: String? = null)

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
