package com.hypergonial.chat.model

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import com.hypergonial.chat.model.payloads.rest.GuildUpdateRequest
import com.hypergonial.chat.model.payloads.rest.UserUpdateRequest
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope

interface Client : InstanceKeeper.Instance, EventManagerAware, CacheAware {
    /**
     * The coroutine scope in use by the client
     *
     * @see replaceScope
     */
    val scope: CoroutineScope

    /**
     * If true, the client is paused and will not perform background tasks
     *
     * @see pause
     * @see resume
     */
    val isSuspended: Boolean

    /**
     * A random session ID for the client to use This is generally used to differentiate between sessions of the same
     * user on different devices.
     */
    val sessionId: String

    /** The amount of times the client has attempted to reconnect to the gateway so far */
    val reconnectAttempts: Int

    /** The maximum amount of times the client will attempt to reconnect to the gateway */
    val maxReconnectAttempts: Int

    /**
     * Replace the coroutine scope of the client with a different one.
     *
     * Stops and restarts all background tasks on the new scope.
     *
     * @see resume
     */
    fun replaceScope(scope: CoroutineScope)

    /**
     * Stops all background tasks and disconnects the client from the gateway.
     *
     * State is preserved and the client can be resumed with [resume].
     *
     * @see resume
     * @see replaceScope
     */
    fun pause()

    /**
     * Resumes the client and restarts all background tasks.
     *
     * The client must be paused with [pause] before it can be resumed.
     *
     * Note that if the coroutine scope became inactive while the client was paused, the client will not be able to
     * resume. ([scope].isActive will be false) In these cases, call [replaceScope] with a new scope before calling this
     * function.
     *
     * @throws com.hypergonial.chat.model.exceptions.ResumeFailureException If the client cannot be resumed
     * @see pause
     * @see replaceScope
     */
    suspend fun resume()

    /**
     * Check if the client is logged in
     *
     * @return True if the client is logged in, false otherwise
     * @see login
     */
    fun isLoggedIn(): Boolean

    /**
     * Try logging in with the provided credentials
     *
     * @param username The username to log in with
     * @param password The password to log in with
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the login fails
     *
     * If successful, the client will store the authorization token and subsequent requests will be authenticated.
     *
     * @see register
     * @see logout
     */
    suspend fun login(username: String, password: Secret<String>)

    /**
     * Log out the currently authenticated user
     *
     * Wipes all client state.
     *
     * @see login
     */
    fun logout()

    /**
     * Register a new user with the provided credentials
     *
     * @param username The username to register with
     * @param password The password to register with
     * @see checkUsernameForAvailability
     * @see login
     */
    suspend fun register(username: String, password: Secret<String>)

    /**
     * Check if the given username is available The endpoint is not authenticated and can be used to check if a username
     * is available before registering.
     *
     * @param username The username to check
     * @return True if the username is available, false otherwise
     */
    suspend fun checkUsernameForAvailability(username: String): Boolean

    /**
     * Update the currently authenticated user
     *
     * @param scope A lambda that modifies the user update request. Any values set will be edited, even if set to null.
     *   Values not set will be unchanged.
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun updateSelf(scope: (UserUpdateRequest.Builder.() -> Unit)): User

    /** Update the Firebase Cloud Messaging token for the currently authenticated user
     *
     * @param fcmToken The new FCM token
     * @param previousToken The previous FCM token, if any
     *  This will be used to remove the previous token from the user's account
     * */
    suspend fun updateFcmToken(fcmToken: String, previousToken: String? = null)

    /**
     * Connects to the gateway
     *
     * This function returns immediately, and the client will connect in the background.
     *
     * @see waitUntilConnected
     * @see isConnected
     * @see waitUntilDisconnected
     */
    fun connect()

    /**
     * Check if the client is connected to the gateway
     *
     * @return True if the client is connected, false otherwise
     * @see connect
     */
    fun isConnected(): Boolean

    /**
     * Wait until the client is connected to the gateway
     *
     * This function will return immediately if the client is already connected.
     *
     * @see connect
     */
    suspend fun waitUntilConnected()

    /**
     * Wait until the client is disconnected from the gateway
     *
     * This function will return immediately if the client is already disconnected.
     *
     * @see closeGateway
     */
    suspend fun waitUntilDisconnected()

    /**
     * Closes the gateway connection, if it is open. This function returns immediately, and the client will disconnect
     * in the background.
     *
     * @see waitUntilDisconnected
     */
    fun closeGateway()

    /**
     * Fetch a batch of messages from the given channel.
     *
     * @param channelId The ID of the channel to fetch messages from
     * @param before Fetch messages before this message ID
     * @param after Fetch messages after this message ID
     * @param limit The maximum number of messages to fetch
     * @return A list of messages fetched from the channel, sorted by creation date in ascending order.
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the channel does not exist
     */
    suspend fun fetchMessages(
        channelId: Snowflake,
        before: Snowflake? = null,
        after: Snowflake? = null,
        around: Snowflake? = null,
        limit: Int = 100,
    ): List<Message>

    /**
     * Set a typing indicator in the given channel. Implementations are expected to keep displaying this for about 6
     * seconds, then remove it.
     *
     * This function is safe to call multiple times in quick succession, as the client will only send the typing
     * indicator every 5 seconds.
     *
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the channel does not exist
     */
    suspend fun setTypingIndicator(channelId: Snowflake)

    /**
     * Acknowledge all messages in the given channel. This is used to mark messages until the current last message as
     * read.
     *
     * This function is safe to call multiple times in quick succession, as the client will only send the ack 2 seconds
     * after the last call, cancelling any previous acks.
     *
     * @param channelId The ID of the channel
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the channel or message does not exist
     * @throws com.hypergonial.chat.model.exceptions.ForbiddenException If the client doesn't have permission to ack
     *   messages in this channel
     */
    fun ackMessages(channelId: Snowflake)

    /**
     * Send a message to the given channel
     *
     * @param channelId The ID of the channel to send the message to
     * @param content The content of the message
     * @param nonce An identifier for the message, can be used to track message delivery MessageCreateEvent and
     *   UploadProgressEvent both contain the nonce of the message they are related to.
     * @param attachments A list of files to upload with the message
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun sendMessage(
        channelId: Snowflake,
        content: String?,
        nonce: String? = null,
        attachments: List<PlatformFile> = emptyList(),
    ): Message

    /**
     * Edit a message by its ID
     *
     * @param channelId The ID of the channel the message is in
     * @param messageId The ID of the message to edit
     * @param content The new content of the message
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun editMessage(channelId: Snowflake, messageId: Snowflake, content: String? = null): Message

    /**
     * Delete a message by its ID
     *
     * @param channelId The ID of the channel the message is in
     * @param messageId The ID of the message to delete
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun deleteMessage(channelId: Snowflake, messageId: Snowflake)

    /**
     * Reloads the client API configuration from persistent storage.
     *
     * This includes all API endpoints, the gateway URL, and other settings.
     */
    fun reloadConfig()

    /**
     * Wait until the client is ready
     *
     * The client is considered ready when it is connected to the gateway and has received all necessary data.
     *
     * This function will return immediately if the client is already ready.
     *
     * @see isReady
     * @see connect
     */
    suspend fun waitUntilReady()

    /**
     * Check if the client is ready
     *
     * The client is considered ready when it is connected to the gateway and has received all necessary data.
     *
     * @return True if the client is ready, false otherwise
     * @see waitUntilReady
     * @see connect
     */
    fun isReady(): Boolean

    /**
     * Fetch a guild by its ID
     *
     * @param guildId The ID of the guild to fetch
     * @return The fetched guild
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun fetchGuild(guildId: Snowflake): Guild

    /**
     * Fetch a channel by its ID
     *
     * @param channelId The ID of the channel to fetch
     * @return The fetched channel
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun fetchChannel(channelId: Snowflake): Channel

    /**
     * Join a new guild as the currently authenticated user
     *
     * @param guildId The ID of the guild to join
     * @return The created member
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun joinGuild(guildId: Snowflake): Member

    /**
     * Creates a new guild with the given name
     *
     * @param name The name of the guild to create
     * @return The created guild
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun createGuild(name: String): Guild

    /**
     * Update a guild by its ID
     *
     * Caution! If you edit the guild's owner, you will lose further permissions to edit or delete the guild.
     *
     * @param guildId The ID of the guild to update
     * @param scope A lambda that modifies the guild update request. Any values set will be edited, even if set to null.
     *   Values not set will be unchanged.
     * @return The updated guild
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the guild does not exist
     * @throws com.hypergonial.chat.model.exceptions.ForbiddenException If the client doesn't own this guild
     */
    suspend fun updateGuild(guildId: Snowflake, scope: (GuildUpdateRequest.Builder.() -> Unit)): Guild

    /**
     * Deletes the guild with the given ID
     *
     * @param id The ID of the guild to delete
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the guild does not exist
     * @throws com.hypergonial.chat.model.exceptions.ForbiddenException If the client doesn't own this guild
     */
    suspend fun deleteGuild(id: Snowflake)

    /**
     * Leaves the guild with the given ID
     *
     * @param id The ID of the guild to leave
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the guild does not exist
     */
    suspend fun leaveGuild(id: Snowflake)

    /**
     * Creates a new channel in the given guild
     *
     * @param guildId The ID of the guild to create the channel in
     * @param name The name of the channel to create
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     */
    suspend fun createChannel(guildId: Snowflake, name: String): Channel

    /**
     * Deletes the channel with the given ID
     *
     * @param channelId The ID of the channel to delete
     * @throws com.hypergonial.chat.model.exceptions.UnauthorizedException If the client is not logged in
     * @throws com.hypergonial.chat.model.exceptions.NotFoundException If the channel does not exist
     * @throws com.hypergonial.chat.model.exceptions.ForbiddenException If the client has missing permissions
     */
    suspend fun deleteChannel(channelId: Snowflake)
}
