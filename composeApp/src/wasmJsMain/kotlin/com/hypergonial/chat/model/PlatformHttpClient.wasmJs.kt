package com.hypergonial.chat.model

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient {
    config(this)
}
