package com.hypergonial.chat.model

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.hypergonial.chat.model.exceptions.AuthorizationFailedException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

interface Client : InstanceKeeper.Instance {
    var token: Secret<String>?

    fun isLoggedIn(): Boolean

    /** Try logging in with the provided credentials */
    suspend fun login(username: String, password: Secret<String>)

    fun logout()

    override fun onDestroy()
}
