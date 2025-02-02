package com.hypergonial.chat.model

// TODO: Evaluate if this is still needed

/*
import com.hypergonial.chat.model.exceptions.AuthorizationFailedException
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MockClient : Client {

    private val messages by lazy {
        (0 until 4000).map {
            Message(
                Snowflake(31557600000u + it.toULong()),
                Snowflake(0u),
                "Message $it: among us",
                User(Snowflake(it.toULong()), "user_$it", displayName = "User $it")
            )
        }.reversed().toMutableList()
    }
    private val readyJob = Job()

    override val eventManager = EventManager()

    override val cache = Cache()

    private var token = settings.getToken()?.let { Secret(it) }

    init {
        cache.putOwnUser(User(
            Snowflake(0u),
            "user_0",
            displayName = "User 0",
        ))
    }

    override fun isLoggedIn(): Boolean {
        return token != null
    }

    override fun isReady(): Boolean {
        return readyJob.isCompleted
    }

    override suspend fun fetchGuild(guildId: Snowflake): Guild {
        TODO("Not yet implemented")
    }

    override suspend fun fetchGuildChannels(guildId: Snowflake): List<Channel> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchChannel(channelId: Snowflake): Channel {
        TODO("Not yet implemented")
    }

    override suspend fun waitUntilReady() {
        readyJob.join()
    }


    */
/** Try logging in with the provided credentials *//*

    override suspend fun login(username: String, password: Secret<String>) {
        delay(1000)

        if (username == "admin" && password.expose() == "admin") {
            token = Secret("test")
            settings.setToken(token!!.expose())
            return
        }

        throw AuthorizationFailedException("Login failed")
    }

    override suspend fun register(username: String, password: Secret<String>) {
        delay(1000)
    }

    override suspend fun connect() {
        delay(500)
    }

    */
/** Fetch a batch of messages from the given channel.
     *
     * @param channelId The channel to fetch messages from.
     * @param before Fetch messages before this message.
     * @param after Fetch messages after this message.
     * @param limit The maximum number of messages to fetch.
     *
     * @return A list of messages.
     *
     * @throws IllegalArgumentException If both before and after are set.
     * *//*

    override suspend fun fetchMessages(
        channelId: Snowflake, before: Snowflake?, after: Snowflake?, limit: UInt
    ): List<Message> {
        delay(500)
        println("Fetching messages before $before after $after limit $limit")

        require(before == null || after == null) { "Only one of before or after can be set" }

        var start = if (before != null) {
            messages.indexOfFirst { it.id == before } + 1
        } else if (after != null) {
            messages.indexOfFirst { it.id == after } - 1
        } else {
            0
        }

        var end = if (before != null) {
            start + limit.toInt()
        } else if (after != null) {
            start - limit.toInt()
        } else {
            start + limit.toInt()
        }

        if (end < start) {
            // swap start and end
            start = end.also { end = start }
        }


        println("Fetching messages from $start to $end")

        return messages.subList(
            start.coerceAtLeast(0).coerceAtMost(messages.size),
            end.coerceAtLeast(0).coerceAtMost(messages.size)
        )
    }

    override suspend fun sendMessage(channelId: Snowflake, content: String, nonce: String?) {
        delay(500)
        val message = Message(
            Snowflake(31557600000u + messages.size.toULong()),
            Snowflake(0u),
            content,
            cache.ownUser!!,
            nonce,
        )

        messages.add(0, message)
        eventManager.dispatch(MessageCreateEvent(message))
    }

    override fun logout() {
        token = null
        settings.removeToken()
    }

    override fun onDestroy() {
        println("MockClient destroyed")
    }

    override suspend fun editMessage(channelId: Snowflake, messageId: Snowflake, content: String?) {
        delay(500)
        messages.forEachIndexed { i, message ->
            if (message.id != messageId)
                return@forEachIndexed

            val newMessage = message.copy(content = content ?: message.content)
            messages[i] = newMessage
            eventManager.dispatch(MessageUpdateEvent(newMessage))
            return
        }
        throw IllegalArgumentException("Message not found: $messageId")
    }
}
*/
