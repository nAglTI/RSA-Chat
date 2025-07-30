package com.hypergonial.chat.data.auth

import com.hypergonial.chat.model.AppSettings
import com.hypergonial.chat.model.payloads.rest.AuthResponse

// TODO then back contract will be ready and move token getter to suspend fun
class AuthManager(private val settings: AppSettings) {
    val accessToken: String?
        get() = settings.getToken()

    suspend fun storeTokens(response: AuthResponse) {

    }
}
