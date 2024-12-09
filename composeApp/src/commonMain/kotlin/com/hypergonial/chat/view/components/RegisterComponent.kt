package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.model.exceptions.ApiException
import kotlinx.coroutines.launch

interface RegisterComponent {
    val data: Value<Data>

    fun onUsernameChange(username: String)

    fun onPasswordChange(password: String)

    fun onPasswordConfirmChange(passwordConfirm: String)

    fun onRegisterAttempt()

    fun onBackClicked()

    data class Data(
        val username: String = "",
        val password: Secret<String> = Secret(""),
        val passwordConfirm: Secret<String> = Secret(""),
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
) : RegisterComponent,
    ComponentContext by ctx {
    override val data = MutableValue(RegisterComponent.Data())
    private val coroutineScope = ctx.coroutineScope()

    /** Query if the login button can be enabled */
    private fun queryCanRegister(): Boolean {
        return data.value.username.isNotEmpty() &&
            data.value.password.expose().isNotEmpty() &&
            data.value.passwordConfirm.expose().isNotEmpty() &&
            queryPasswordErrors(data.value.password, data.value.passwordConfirm).isEmpty()
    }

    private fun queryPasswordErrors(
        password: Secret<String>,
        passwordConfirm: Secret<String>
    ): List<String> {
        if (
            passwordConfirm.expose().isNotEmpty() &&
            password.expose() != passwordConfirm.expose()
        ) {
            println("Passwords do not match")
            println(password.expose())
            println(passwordConfirm.expose())
            println(password.expose() == passwordConfirm.expose())
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
        data.value = data.value.copy(username = username.trim(), canRegister = queryCanRegister())
    }

    override fun onPasswordChange(password: String) {
        data.value = data.value.copy(
            password = Secret(password.trim()),
            canRegister = queryCanRegister(),
            passwordErrors = queryPasswordErrors(
                Secret(password.trim()),
                data.value.passwordConfirm
            )
        )
    }

    override fun onPasswordConfirmChange(passwordConfirm: String) {
        data.value = data.value.copy(
            passwordConfirm = Secret(passwordConfirm.trim()),
            canRegister = queryCanRegister(),
            passwordErrors = queryPasswordErrors(
                data.value.password,
                Secret(passwordConfirm.trim())
            )
        )
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

        coroutineScope.launch {
            try {
                client.register(username, password)
                data.value = data.value.copy(isRegistering = false, registrationFailed = false)
                onRegister()
            } catch (e: ApiException) {
                data.value = data.value.copy(isRegistering = false, registrationFailed = true)
            }

        }
    }

}
