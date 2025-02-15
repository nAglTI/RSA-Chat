package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.hypergonial.chat.ensureSlashAtEnd
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.DevSettings
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.view.content.DebugSettingsContent

interface DebugSettingsComponent : Displayable {
    val data: Value<Data>

    /**
     * Called when the API endpoint URL changes
     *
     * @param url The new URL
     */
    fun onApiEndpointChange(url: String)

    /**
     * Called when the gateway endpoint URL changes
     *
     * @param url The new URL
     */
    fun onGatewayEndpointChange(url: String)

    /**
     * Called when the object store endpoint URL changes
     *
     * @param url The new URL
     */
    fun onObjectStoreEndpointChange(url: String)

    /**
     * Called when the developer mode switch changes
     *
     * @param isInDeveloperMode True if the app is in developer mode
     */
    fun onDeveloperModeChange(isInDeveloperMode: Boolean)

    /** Called when the save button is clicked */
    fun onSaveClicked()

    /** Called when the back button is clicked */
    fun onBackClicked()

    @Composable override fun Display() = DebugSettingsContent(this)

    data class Data(
        /** The API endpoint URL */
        val apiEndpoint: String,
        /** The gateway endpoint URL */
        val gatewayEndpoint: String,
        /** The object store endpoint URL */
        val objectStoreEndpoint: String,
        /** True if the app is in developer mode */
        val isInDeveloperMode: Boolean = false,
        /** True if any of the URLs has changed */
        val hasChanged: Boolean = false,
        /** If true, the API endpoint URL is invalid */
        val apiEndpointError: Boolean = false,
        /** If true, the gateway endpoint URL is invalid */
        val gatewayEndpointError: Boolean = false,
        /** If true, the object store endpoint URL is invalid */
        val objectStoreEndpointError: Boolean = false,
    ) {
        companion object {
            /** Load the API configuration from persistent storage */
            fun load(): Data {
                val config = settings.getDevSettings()
                return Data(config.apiUrl, config.gatewayUrl, config.objectStoreUrl, config.isInDeveloperMode)
            }
        }

        /** Save the API configuration to persistent storage */
        fun save() {
            settings.setDevSettings(
                DevSettings(
                    apiEndpoint.ensureSlashAtEnd(),
                    gatewayEndpoint.ensureSlashAtEnd(),
                    objectStoreEndpoint.ensureSlashAtEnd(),
                    isInDeveloperMode,
                )
            )
        }
    }
}

/**
 * The default implementation of the DebugSettingsComponent
 *
 * @param ctx The component context
 * @param client The client to use for API calls
 * @param onBack The callback to call when the back button is clicked
 */
class DefaultDebugSettingsComponent(val ctx: ComponentContext, val client: Client, val onBack: () -> Unit) :
    DebugSettingsComponent, ComponentContext by ctx {
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

    override fun onDeveloperModeChange(isInDeveloperMode: Boolean) {
        data.value = data.value.copy(isInDeveloperMode = isInDeveloperMode, hasChanged = true)
    }

    override fun onBackClicked() = onBack()

    private fun validate() {
        data.value =
            data.value.copy(
                apiEndpointError = !data.value.apiEndpoint.isHttpUrl(),
                gatewayEndpointError = !data.value.gatewayEndpoint.isWebSocketUrl(),
                objectStoreEndpointError = !data.value.objectStoreEndpoint.isHttpUrl(),
            )
    }

    private fun canSave(): Boolean {
        return !data.value.apiEndpointError &&
            !data.value.gatewayEndpointError &&
            !data.value.objectStoreEndpointError &&
            data.value.hasChanged
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
