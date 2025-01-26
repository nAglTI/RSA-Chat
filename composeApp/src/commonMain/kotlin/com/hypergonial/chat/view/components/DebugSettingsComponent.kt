package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.hypergonial.chat.model.ApiConfig
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.view.content.DebugSettingsContent

interface DebugSettingsComponent: Displayable {
    val data: Value<Data>

    fun onApiEndpointChange(url: String)

    fun onGatewayEndpointChange(url: String)

    fun onObjectStoreEndpointChange(url: String)

    fun onSaveClicked()

    fun onBackClicked()

    @Composable
    override fun Display() = DebugSettingsContent(this)

    data class Data(
        val apiEndpoint: String,
        val gatewayEndpoint: String,
        val objectStoreEndpoint: String,
        val hasChanged: Boolean = false,
        val apiEndpointError: Boolean = false,
        val gatewayEndpointError: Boolean = false,
        val objectStoreEndpointError: Boolean = false,
    ) {
        companion object {
            fun load(): Data {
                val config = settings.getApiSettings()
                return Data(config.apiUrl, config.gatewayUrl, config.objectStoreUrl)
            }
        }

        fun save() {
            settings.setApiSettings(
                ApiConfig(
                    apiEndpoint.ensureSlashAtEnd(),
                    gatewayEndpoint.ensureSlashAtEnd(),
                    objectStoreEndpoint.ensureSlashAtEnd()
                )
            )
        }
    }
}

class DefaultDebugSettingsComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onBack: () -> Unit,
) : DebugSettingsComponent, ComponentContext by ctx {
    override val data = MutableValue(DebugSettingsComponent.Data.load())

    override fun onApiEndpointChange(url: String) {
        data.value = data.value.copy(apiEndpoint = url, hasChanged = true)
        validate()
    }

    override fun onGatewayEndpointChange(url: String) {
        data.value = data.value.copy(gatewayEndpoint = url, hasChanged = true)
        validate()
    }

    override fun onObjectStoreEndpointChange(url: String) {
        data.value = data.value.copy(objectStoreEndpoint = url, hasChanged = true)
        validate()
    }

    override fun onBackClicked() = onBack()

    fun validate() {
        data.value = data.value.copy(
            apiEndpointError = !data.value.apiEndpoint.isHttpUrl(),
            gatewayEndpointError = !data.value.gatewayEndpoint.isWebSocketUrl(),
            objectStoreEndpointError = !data.value.objectStoreEndpoint.isHttpUrl()
        )
    }

    private fun canSave(): Boolean {
        return !data.value.apiEndpointError
            && !data.value.gatewayEndpointError
            && !data.value.objectStoreEndpointError
            && data.value.hasChanged
    }

    override fun onSaveClicked() {
        if (!canSave()) return

        data.value.save()
        data.value = data.value.copy(hasChanged = false)
    }
}

fun String.isHttpUrl(): Boolean {
    return this.startsWith("http://") || this.startsWith("https://")
}

fun String.isWebSocketUrl(): Boolean {
    return this.startsWith("ws://") || this.startsWith("wss://")
}

fun String.ensureSlashAtEnd(): String {
    return if (!this.endsWith("/")) "$this/" else this
}
