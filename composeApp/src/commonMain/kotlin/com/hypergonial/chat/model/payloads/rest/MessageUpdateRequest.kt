package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.Serializable

@Serializable
data class MessageUpdateRequest(
    val content: String? = null
)
