package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateRequest(val username: String? = null, val displayName: String? = null, val avatar: String? = null)
