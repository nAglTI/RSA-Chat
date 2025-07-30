package com.hypergonial.chat.data.remote

import com.hypergonial.chat.data.auth.AuthManager
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.rest.AuthResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

interface ChatApiService {
    suspend fun login(username: String, password: String): AuthResponse
    suspend fun refreshToken(refreshToken: String): AuthResponse
    suspend fun sendMessage(channelId: String, payload: String): Message
    suspend fun fetchMessages(channelId: String, sinceId: String?): List<Message>
}

// TODO: update with back logic + mb remove baseUrl from constructor
class ChatApiServiceImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authManager: AuthManager
) : ChatApiService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun login(username: String, password: String): AuthResponse {
        val response: AuthResponse = httpClient.post("$baseUrl/users/auth") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body(json)
        authManager.storeTokens(response)
        return response
    }

    override suspend fun refreshToken(refreshToken: String): AuthResponse {
        val response: AuthResponse = httpClient.post("$baseUrl/users/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }.body(json)
        authManager.storeTokens(response)
        return response
    }

    override suspend fun sendMessage(channelId: String, payload: String): Message {
        return httpClient.post("$baseUrl/channels/$channelId/messages") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${authManager.accessToken}")
            setBody(payload)
        }.body(json)
    }

    override suspend fun fetchMessages(channelId: String, sinceId: String?): List<Message> {
        return httpClient.get("$baseUrl/channels/$channelId/messages") {
            header(HttpHeaders.Authorization, "Bearer ${authManager.accessToken}")
            parameter("since", sinceId)
        }.body(json)
    }
}
