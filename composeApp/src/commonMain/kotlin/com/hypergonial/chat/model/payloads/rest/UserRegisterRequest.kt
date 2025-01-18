package com.hypergonial.chat.model.payloads.rest

import com.hypergonial.chat.model.Secret
import kotlinx.serialization.Serializable

@Serializable
data class UserRegisterRequest(val username: String, val password: Secret<String>)
