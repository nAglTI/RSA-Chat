package com.hypergonial.chat.model.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An attachment to a message.
 *
 * @param id The ID of the attachment.
 * @param filename The name of the file.
 * @param contentType The MIME type of the file.
 */
@Serializable
data class Attachment(val id: Int, val filename: String, @SerialName("content_type") val contentType: String)
