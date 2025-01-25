package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.Serializable

@Serializable
data class MessageCreateRequest(
    val content: String,
    val nonce: String? = null,
)
