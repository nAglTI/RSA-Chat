package com.hypergonial.chat.model

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient
