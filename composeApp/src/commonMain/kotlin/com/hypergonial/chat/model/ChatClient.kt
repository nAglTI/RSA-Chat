package com.hypergonial.chat.model

class ChatClient(override var token: Secret<String>? = null): Client {
    override fun isLoggedIn(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun login(username: String, password: Secret<String>) {
        TODO("Not yet implemented")
    }

    override fun logout() {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }
}
