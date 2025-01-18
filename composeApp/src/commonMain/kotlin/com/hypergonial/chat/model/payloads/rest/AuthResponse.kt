package com.hypergonial.chat.model.payloads.rest

import com.hypergonial.chat.model.Secret
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(val username: String, val token: Secret<String>)
