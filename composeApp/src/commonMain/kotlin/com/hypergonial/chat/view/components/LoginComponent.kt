package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.model.exceptions.UnauthorizedException
import com.hypergonial.chat.view.content.LoginContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel as QueueChannel
import kotlinx.coroutines.channels.ReceiveChannel as QueueReceiveChannel
import kotlinx.coroutines.launch

interface LoginComponent: Displayable {
    val data: Value<Data>

    fun onUsernameChange(username: String)

    fun onPasswordChange(password: String)

    fun onLoginAttempt()

    fun onRegisterRequested()

    fun onLogoClick()

    @Composable
    override fun Display() = LoginContent(this)

    val errors: QueueReceiveChannel<String>

    data class Data(
        val username: String = "",
        val password: Secret<String> = Secret(""),
        val canLogin: Boolean = false,
        val isLoggingIn: Boolean = false,
        val loginFailed: Boolean = false,
        val logoClickCount: Int = 0,
    )
}

class DefaultLoginComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onLogin: () -> Unit,
    val onRegisterRequest: () -> Unit,
    val onDebugSettingsOpen: () -> Unit
) : LoginComponent,
    ComponentContext by ctx {
    override val data = MutableValue(LoginComponent.Data())
    override val errors = QueueChannel<String>(1)
    private val scope = ctx.coroutineScope()
    private val logger = KotlinLogging.logger {}

    /** Query if the login button can be enabled */
    private fun queryCanLogin(): Boolean {
        return data.value.username.isNotEmpty() && data.value.password.expose().isNotEmpty()
    }

    override fun onUsernameChange(username: String) {
        data.value = data.value.copy(username = username.lowercase(), canLogin = queryCanLogin())
    }

    override fun onPasswordChange(password: String) {
        data.value = data.value.copy(password = Secret(password), canLogin = queryCanLogin())
    }

    override fun onLogoClick() {
        data.value = data.value.copy(logoClickCount = data.value.logoClickCount + 1)
        if (data.value.logoClickCount >= 8) {
            data.value = data.value.copy(logoClickCount = 0)
            onDebugSettingsOpen()
        }
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

        scope.launch {
            try {
                client.login(username, password)
                data.value = data.value.copy(isLoggingIn = false, loginFailed = false)
                onLogin()
            } catch (_: UnauthorizedException) {
                data.value = data.value.copy(isLoggingIn = false, loginFailed = true)
            }
            catch(e: Exception) {
                logger.error { "Login failed: ${e.message}" }
                val res = errors.trySend("Failed to connect, please try again later.")
                data.value = data.value.copy(isLoggingIn = false, loginFailed = true)
            }
        }
    }

    override fun onRegisterRequested() {
        onRegisterRequest()
    }

}
