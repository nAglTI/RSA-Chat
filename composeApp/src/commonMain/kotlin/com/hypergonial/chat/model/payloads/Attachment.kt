package com.hypergonial.chat.model.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: Int,
    val filename: String,
    @SerialName("content_type")
    val contentType: String,
)
