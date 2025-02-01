package com.hypergonial.chat.model

import com.hypergonial.chat.IS_DEVELOPMENT_BUILD
import kotlinx.serialization.Serializable

@Serializable
data class ApiConfig(
    val apiUrl: String,
    val gatewayUrl: String,
    val objectStoreUrl: String,
) {
    companion object {
        fun default(): ApiConfig {
            return ApiConfig(
                apiUrl = "https://chat.hypergonial.com/api/v1",
                gatewayUrl = "wss://chat.hypergonial.com/gateway/v1",
                objectStoreUrl = "https://chat-cdn.hypergonial.com",
            )
        }
    }
}
