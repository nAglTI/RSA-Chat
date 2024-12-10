package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake

class ChatClient: Client {
    private var token: Secret<String>? = settings.getToken()?.let { Secret(it) }

    override fun isLoggedIn(): Boolean {
        return token != null
    }


    override suspend fun login(username: String, password: Secret<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun register(username: String, password: Secret<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun fetchMessages(
        channelId: Snowflake,
        before: Snowflake?,
        after: Snowflake?,
        limit: UInt
    ): List<Message> {
        TODO("Not yet implemented")
    }

    override fun logout() {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }
}
