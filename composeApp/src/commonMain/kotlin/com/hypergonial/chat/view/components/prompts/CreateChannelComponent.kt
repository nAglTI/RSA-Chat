package com.hypergonial.chat.view.components.prompts

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.EffectContainer
import com.hypergonial.chat.containAsEffect
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.exceptions.ClientException
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.components.Displayable
import com.hypergonial.chat.view.content.prompts.CreateChannelContent
import kotlinx.coroutines.launch

interface CreateChannelComponent : Displayable {
    fun onCreateChannelClicked()

    fun onBackClicked()

    fun onChannelNameChanged(channelName: String)

    @Composable override fun Display() = CreateChannelContent(this)

    val data: Value<State>

    data class State(
        val channelName: String = "",
        val isCreateButtonEnabled: Boolean = false,
        val isLoading: Boolean = false,
        val snackbarMessage: EffectContainer<String> = "".containAsEffect(),
    )
}

class DefaultCreateChannelComponent(
    val ctx: ComponentContext,
    val client: Client,
    val guildId: Snowflake,
    val onCreated: (Channel) -> Unit,
    val onCancel: () -> Unit,
) : CreateChannelComponent {
    override val data = MutableValue(CreateChannelComponent.State())

    private val scope = ctx.coroutineScope()
    private val logger = Logger.withTag("DefaultCreateChannelComponent")

    override fun onCreateChannelClicked() {
        scope.launch {
            data.value = data.value.copy(isLoading = true)
            val channel =
                try {
                    client.createChannel(guildId, data.value.channelName)
                } catch (e: ClientException) {
                    logger.e { "Failed to create channel: ${e.message}" }
                    data.value =
                        data.value.copy(
                            isLoading = false,
                            snackbarMessage =
                                (e.message ?: "An error occurred, please try again later.").containAsEffect(),
                        )
                    return@launch
                }

            data.value = data.value.copy(isLoading = false)
            onCreated(channel)
        }
    }

    override fun onChannelNameChanged(channelName: String) {
        data.value =
            data.value.copy(
                channelName = channelName.replace(Regex("\\s+"), "_").lowercase().trim().take(32),
                isCreateButtonEnabled = channelName.isNotBlank(),
            )
    }

    override fun onBackClicked() {
        onCancel()
    }
}
