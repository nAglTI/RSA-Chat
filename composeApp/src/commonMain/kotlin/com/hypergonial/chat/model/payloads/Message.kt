package com.hypergonial.chat.model.payloads

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Snowflake,
    @SerialName("channel_id")
    val channelId: Snowflake,
    val content: String? = null,
    val author: PartialUser,
    val nonce: String? = null,
    val attachments: List<Attachment> = emptyList(),
) {
    val createdAt: Instant
        get() = id.createdAt
}
