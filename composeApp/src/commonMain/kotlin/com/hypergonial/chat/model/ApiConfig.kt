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
        val DEV = ApiConfig(
            gatewayUrl = "ws://localhost:8080/gateway/v1",
            apiUrl = "http://localhost:8080/api/v1",
            objectStoreUrl = "http://localhost:9000",
        )

        val PROD = ApiConfig(
            apiUrl = "https://chat.hypergonial.com/api/v1",
            gatewayUrl = "wss://chat.hypergonial.com/gateway/v1",
            objectStoreUrl = "https://chat-cdn.hypergonial.com",
        )

        fun default(): ApiConfig {
            return if (IS_DEVELOPMENT_BUILD) {
                DEV
            } else {
                PROD
            }
        }
    }
}
