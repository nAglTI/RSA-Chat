package com.hypergonial.chat.model

import com.hypergonial.chat.model.exceptions.AuthorizationFailedException
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.User
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.random.nextULong

class MockClient : Client {
    private var batchId = 0

    override fun isLoggedIn(): Boolean {
        return token != null
    }

    private var token = settings.getToken()?.let { Secret(it) }

    /** Try logging in with the provided credentials */
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

    override suspend fun fetchMessages(
        channelId: Snowflake,
        before: Snowflake?,
        after: Snowflake?,
        limit: UInt
    ): List<Message> {
        delay(1000)
        batchId += 1
        return (0 until limit.toInt()).map {
            Message(
                Snowflake(Random.nextULong(31557600000u, ULong.MAX_VALUE)),
                "Message $it - Batch $batchId",
                User(Snowflake(it.toULong()), "user_$it", displayName = "User $it")
            )
        }
    }

    override fun logout() {
        token = null
        settings.removeToken()
    }

    override fun onDestroy() {
        println("MockClient destroyed")
    }
}
