package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.model.exceptions.ApiException
import com.hypergonial.chat.view.content.RegisterContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch

interface RegisterComponent: Displayable {
    val data: Value<Data>

    fun onUsernameChange(username: String)

    fun onPasswordChange(password: String)

    fun onPasswordConfirmChange(passwordConfirm: String)

    fun onRegisterAttempt()

    fun onBackClicked()

    @Composable
    override fun Display() = RegisterContent(this)

    data class Data(
        val username: String = "",
        val password: Secret<String> = Secret(""),
        val passwordConfirm: Secret<String> = Secret(""),
        val usernameErrors: List<String> = listOf(),
        val passwordErrors: List<String> = listOf(),
        val canRegister: Boolean = false,
        val isRegistering: Boolean = false,
        val registrationFailed: Boolean = false,
    )
}

class DefaultRegisterComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onRegister: () -> Unit,
    val onBack: () -> Unit
) : RegisterComponent, ComponentContext by ctx {
    override val data = MutableValue(RegisterComponent.Data())
    private val scope = ctx.coroutineScope()
    private val usernameRegex = Regex("^([a-z0-9]|[a-z0-9]+(?:[._][a-z0-9]+)*)\$")
    private val logger = KotlinLogging.logger {}

    /** Query if the login button can be enabled */
    private fun updateCanRegister() {
        data.value = data.value.copy(
            canRegister = data.value.username.isNotEmpty() && data.value.password.expose()
                .isNotEmpty() && data.value.passwordConfirm.expose()
                .isNotEmpty() && getPasswordErrors(
                data.value.password,
                data.value.passwordConfirm
            ).isEmpty() && getUsernameErrors(data.value.username).isEmpty()
        )
    }

    private fun updatePasswordErrors() {
        data.value = data.value.copy(
            passwordErrors = getPasswordErrors(
                data.value.password, data.value.passwordConfirm
            )
        )
    }

    private fun updateUsernameErrors() {
        data.value = data.value.copy(usernameErrors = getUsernameErrors(data.value.username))
    }

    private fun getUsernameErrors(username: String): List<String> {
        if (username.length < 3) {
            return listOf("Username must be at least 3 characters")
        }

        if (username.length > 32) {
            return listOf("Username must be at most 32 characters")
        }

        if (!usernameRegex.matches(username)) {
            if (username.endsWith("_") || username.endsWith(".")) {
                return listOf("Username must not end with an underscore or period")
            }

            if (username.startsWith("_") || username.startsWith(".")) {
                return listOf("Username must not start with an underscore or period")
            }

            if (username.contains(" ")) {
                return listOf("Username must not contain spaces")
            }

            return listOf("Username must only contain lowercase letters, numbers, underscores, and periods")
        }

        return emptyList()
    }

    private fun getPasswordErrors(
        password: Secret<String>, passwordConfirm: Secret<String>
    ): List<String> {
        if (passwordConfirm.expose()
                .isNotEmpty() && password.expose() != passwordConfirm.expose()
        ) {
            return listOf("Passwords do not match")
        }

        val errors = mutableListOf<String>()

        if (password.expose().length < 8) {
            errors.add("Password must be at least 8 characters")
        }

        // One for the pass-phrase people
        if (password.expose().split(" ").size > 20) {
            return errors
        }

        if (!password.expose().any { it.isDigit() }) {
            errors.add("Password must contain at least one number")
        }

        if (!password.expose().any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }

        if (!password.expose().any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }

        if (!password.expose().any { !it.isLetterOrDigit() }) {
            errors.add("Password must contain at least one special character")
        }

        return errors
    }

    override fun onUsernameChange(username: String) {
        data.value = data.value.copy(username = username.trim().take(32))
        updateUsernameErrors()
        updateCanRegister()
    }

    override fun onPasswordChange(password: String) {
        data.value = data.value.copy(
            password = Secret(password.trim()),
        )
        updatePasswordErrors()
        updateCanRegister()
    }

    override fun onPasswordConfirmChange(passwordConfirm: String) {
        data.value = data.value.copy(
            passwordConfirm = Secret(passwordConfirm.trim()),
        )
        updatePasswordErrors()
        updateCanRegister()
    }

    override fun onBackClicked() = onBack()

    override fun onRegisterAttempt() {
        // Save password before removal from UI
        val username = data.value.username.trim()
        val password = data.value.password.map { it.trim() }

        data.value = data.value.copy(
            password = Secret(""),
            passwordConfirm = Secret(""),
            isRegistering = true,
            registrationFailed = false,
            canRegister = false
        )

        scope.launch {
            val availability = client.checkUsernameForAvailability(username)
            println("Availability: $availability")
            if (!availability) {
                data.value = data.value.copy(
                    isRegistering = false,
                    registrationFailed = true,
                    usernameErrors = listOf("Username is already taken")
                )
                return@launch
            }

            try {
                client.register(username, password)
                data.value = data.value.copy(isRegistering = false, registrationFailed = false)
                onRegister()
            } catch (_: ApiException) {
                data.value = data.value.copy(isRegistering = false, registrationFailed = true)
            }
            catch (e: Exception) {
                logger.error { "Registration failed: ${e.message}" }
                data.value = data.value.copy(isRegistering = false, registrationFailed = true)
            }

        }
    }

}
