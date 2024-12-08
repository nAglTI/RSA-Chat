package com.hypergonial.chat.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

interface LoginComponent {
    val data: Value<Data>

    fun onUsernameChanged(username: String)

    fun onPasswordChanged(password: String)

    fun onLogin()

    data class Data(
        val username: String,
        val password: String
    )
}

class DefaultLoginComponent(val ctx: ComponentContext, val onLoginComplete: () -> Unit) : LoginComponent {
    override val data = MutableValue(LoginComponent.Data("", ""))

    override fun onUsernameChanged(username: String) {
        data.value = data.value.copy(username = username)
    }

    override fun onPasswordChanged(password: String) {
        data.value = data.value.copy(password = password)
    }

    override fun onLogin() {
        data.value = data.value.copy(username = "", password = "")
        onLoginComplete()
    }

}
