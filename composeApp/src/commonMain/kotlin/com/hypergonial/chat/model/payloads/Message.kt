package com.hypergonial.chat.model.payloads

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A message in a channel.
 *
 * @param id The ID of the message.
 * @param channelId The ID of the channel the message is in.
 * @param author The author of the message. This may be a member or a user.
 * @param content The content of the message.
 * @param nonce The nonce of the message.
 * This can be set by the client to track message delivery,
 * and is returned by the gateway upon a successful MESSAGE_CREATE.
 * @param isEdited Whether the message has been edited before.
 * @param attachments The attachments of the message.
 * */
@Serializable
data class Message(
    val id: Snowflake,
    @SerialName("channel_id")
    val channelId: Snowflake,
    val author: PartialUser,
    val content: String? = null,
    val nonce: String? = null,
    @SerialName("edited")
    val isEdited: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
) {
    /** The time the message was created at. */
    val createdAt: Instant
        get() = id.createdAt
}
