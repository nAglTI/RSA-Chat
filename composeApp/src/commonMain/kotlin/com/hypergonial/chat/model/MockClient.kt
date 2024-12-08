package com.hypergonial.chat.model

import com.hypergonial.chat.model.exceptions.AuthorizationFailedException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

class MockClient(override var token: Secret<String>? = null) : Client {
    init {
        println("Client created, is logged in: ${isLoggedIn()}")
    }

    /** Try logging in with the provided credentials */
    override suspend fun login(username: String, password: Secret<String>) {
        delay(1000)

        if (username == "admin" && password.expose() == "admin") {
            token = Secret("test")
            return
        }

        throw AuthorizationFailedException("Login failed")
    }

    override fun logout() {
        token = null
    }

    override fun onDestroy() {
        /** TODO: Close http client */
        println("Client destroyed")
    }
}
