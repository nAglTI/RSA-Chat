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

/**
 * The login component
 *
 * This is the default screen displayed when the user is not logged in
 */
interface LoginComponent : Displayable {
    val data: Value<Data>

    /**
     * Called when the username changes
     *
     * @param username The new username
     */
    fun onUsernameChange(username: String)

    /**
     * Called when the password changes
     *
     * @param password The new password
     */
    fun onPasswordChange(password: String)

    /** Called when the login button is clicked */
    fun onLoginAttempt()

    /** Called when the register button is clicked */
    fun onRegisterRequested()

    /** Called when the logo is clicked This is used to open the debug menu */
    fun onLogoClick()

    @Composable override fun Display() = LoginContent(this)

    /** The error channel for retrieval errors */
    val errors: QueueReceiveChannel<String>

    data class Data(
        /** The username entered by the user */
        val username: String = "",
        /** The password entered by the user */
        val password: Secret<String> = Secret(""),
        /** If true, the login button can be enabled */
        val canLogin: Boolean = false,
        /** If true, the login is in progress */
        val isLoggingIn: Boolean = false,
        /** If true, the login failed */
        val loginFailed: Boolean = false,
        /** The number of times the logo has been clicked This is used to open the debug menu */
        val logoClickCount: Int = 0,
    )
}

/**
 * The default implementation of the login component
 *
 * @param ctx The component context
 * @param client The client to use for API calls
 * @param onLogin The callback to call when the user logs in
 * @param onRegisterRequest The callback to call when the user requests to register
 * @param onDebugSettingsOpen The callback to call when the user requests to open the debug settings
 */
class DefaultLoginComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onLogin: () -> Unit,
    val onRegisterRequest: () -> Unit,
    val onDebugSettingsOpen: () -> Unit,
) : LoginComponent, ComponentContext by ctx {
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

        data.value = data.value.copy(password = Secret(""), isLoggingIn = true, loginFailed = false, canLogin = false)

        scope.launch {
            try {
                client.login(username, password)
                data.value = data.value.copy(isLoggingIn = false, loginFailed = false)
                onLogin()
            } catch (_: UnauthorizedException) {
                data.value = data.value.copy(isLoggingIn = false, loginFailed = true)
            } catch (e: Exception) {
                logger.error { "Login failed: ${e.message}" }
                errors.trySend("Failed to connect, please try again later.")
                data.value = data.value.copy(isLoggingIn = false, loginFailed = true)
            }
        }
    }

    override fun onRegisterRequested() {
        onRegisterRequest()
    }
}
