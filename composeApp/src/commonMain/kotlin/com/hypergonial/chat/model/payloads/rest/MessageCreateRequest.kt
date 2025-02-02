package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.Serializable

/** Request to create a message.
 *
 * Belongs to: POST /api/v1/channels/{channelId}/messages
 *
 * @param content The content of the message.
 * @param nonce A nonce to identify the message.
 * */
@Serializable
data class MessageCreateRequest(
    val content: String,
    val nonce: String? = null,
)
