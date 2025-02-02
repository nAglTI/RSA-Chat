package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.Serializable

/** Request to update a message.
 *
 * Belongs to: PATCH /api/v1/channels/{channelId}/messages/{messageId}
 *
 * @param content The new content of the message.
 * */
@Serializable
data class MessageUpdateRequest(val content: String? = null)
