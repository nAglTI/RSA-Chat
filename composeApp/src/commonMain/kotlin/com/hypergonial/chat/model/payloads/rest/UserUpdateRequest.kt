package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateRequest(
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val avatar: String? = null,
)
