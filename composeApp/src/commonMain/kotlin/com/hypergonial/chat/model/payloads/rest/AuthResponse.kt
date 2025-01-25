package com.hypergonial.chat.model.payloads.rest

import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(@SerialName("user_id") val userId: Snowflake, val token: Secret<String>)
