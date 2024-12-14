package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.MessageCreateEvent
import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.model.exceptions.AuthorizationFailedException
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import kotlinx.coroutines.launch

interface LoginComponent {
    val data: Value<Data>

    fun onUsernameChange(username: String)

    fun onPasswordChange(password: String)

    fun onLoginAttempt()

    fun onRegisterRequested()

    data class Data(
        val username: String = "",
        val password: Secret<String> = Secret(""),
        val canLogin: Boolean = false,
        val isLoggingIn: Boolean = false,
        val loginFailed: Boolean = false,
    )
}

class DefaultLoginComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onLogin: () -> Unit,
    val onRegisterRequest: () -> Unit,
) : LoginComponent,
    ComponentContext by ctx {
    override val data = MutableValue(LoginComponent.Data())
    private val coroutineScope = ctx.coroutineScope()

    /** Query if the login button can be enabled */
    private fun queryCanLogin(): Boolean {
        return data.value.username.isNotEmpty() && data.value.password.expose().isNotEmpty()
    }

    override fun onUsernameChange(username: String) {
        data.value = data.value.copy(username = username, canLogin = queryCanLogin())
    }

    override fun onPasswordChange(password: String) {
        data.value = data.value.copy(password = Secret(password), canLogin = queryCanLogin())
    }

    override fun onLoginAttempt() {
        // Save password before removal from UI
        val username = data.value.username.trim()
        val password = data.value.password.map { it.trim() }

        data.value = data.value.copy(
            password = Secret(""),
            isLoggingIn = true,
            loginFailed = false,
            canLogin = false
        )

        coroutineScope.launch {
            try {
                client.login(username, password)
                data.value = data.value.copy(isLoggingIn = false, loginFailed = false)
                onLogin()
            } catch (e: AuthorizationFailedException) {
                data.value = data.value.copy(isLoggingIn = false, loginFailed = true)
            }
        }
    }

    override fun onRegisterRequested() {
        onRegisterRequest()
    }

}
