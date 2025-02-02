package com.hypergonial.chat.model

import kotlinx.serialization.Serializable

/**
 * The API configuration for the client.
 *
 * @param apiUrl The URL of the API. It typically ends in `/api/v1/`.
 * @param gatewayUrl The URL of the gateway. It typically ends in `/gateway/v1`.
 * @param objectStoreUrl The URL of the S3 object store.
 */
@Serializable
data class ApiConfig(val apiUrl: String, val gatewayUrl: String, val objectStoreUrl: String) {
    companion object {

        /**
         * Get the default API configuration.
         *
         * @return The default API configuration.
         */
        fun default(): ApiConfig {
            return ApiConfig(
                apiUrl = "https://chat.hypergonial.com/api/v1/",
                gatewayUrl = "wss://chat.hypergonial.com/gateway/v1",
                objectStoreUrl = "https://chat-cdn.hypergonial.com/",
            )
        }
    }
}
