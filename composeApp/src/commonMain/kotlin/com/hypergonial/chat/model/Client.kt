package com.hypergonial.chat.model

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.hypergonial.chat.model.exceptions.AuthorizationFailedException
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

interface Client : InstanceKeeper.Instance, EventManagerAware, CacheAware {
    fun isLoggedIn(): Boolean

    /** Try logging in with the provided credentials */
    suspend fun login(username: String, password: Secret<String>)

    /** Register a new user with the provided credentials */
    suspend fun register(username: String, password: Secret<String>)

    /** Fetch a batch of messages from the given channel. */
    suspend fun fetchMessages(
        channelId: Snowflake,
        before: Snowflake? = null,
        after: Snowflake? = null,
        limit: UInt = 100u
    ): List<Message>

    suspend fun sendMessage(channelId: Snowflake, content: String, nonce: String? = null)

    fun logout()

    override fun onDestroy() {
        // TODO: Close http client
    }

    suspend fun editMessage(channelId: Snowflake, messageId: Snowflake, content: String? = null)
}
