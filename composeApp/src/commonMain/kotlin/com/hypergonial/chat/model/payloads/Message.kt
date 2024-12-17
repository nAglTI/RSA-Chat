package com.hypergonial.chat.model.payloads

import kotlinx.datetime.Instant

data class Message(
    val id: Snowflake,
    val channelId: Snowflake,
    val content: String? = null,
    val author: PartialUser,
    val nonce: String? = null
) {
    val createdAt: Instant
        get() = id.createdAt
}
