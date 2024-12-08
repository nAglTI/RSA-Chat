package com.hypergonial.chat.model

import com.hypergonial.chat.model.exceptions.AuthorizationFailedException
import kotlinx.coroutines.delay

class MockClient : Client {
    override fun isLoggedIn(): Boolean {
        return token != null
    }

    private var token = settings.getToken()?.let { Secret(it) }

    /** Try logging in with the provided credentials */
    override suspend fun login(username: String, password: Secret<String>) {
        delay(1000)

        if (username == "admin" && password.expose() == "admin") {
            token = Secret("test")
            settings.setToken(token!!.expose())
            return
        }

        throw AuthorizationFailedException("Login failed")
    }

    override fun logout() {
        token = null
        settings.setToken("")
    }

    override fun onDestroy() {
        /** TODO: Close http client */
        println("Client destroyed")
    }
}
