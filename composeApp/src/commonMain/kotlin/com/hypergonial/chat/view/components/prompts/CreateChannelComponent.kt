package com.hypergonial.chat.view.components.prompts

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.components.Displayable
import com.hypergonial.chat.view.content.prompts.CreateChannelContent
import kotlinx.coroutines.launch

interface CreateChannelComponent: Displayable {
    fun onCreateChannelClicked()

    fun onBackClicked()

    fun onChannelNameChanged(channelName: String)

    @Composable
    override fun Display() = CreateChannelContent(this)

    val data: Value<State>

    data class State(
        val channelName: String = "",
        val isCreateButtonEnabled: Boolean = false,
        val isLoading: Boolean = false
    )
}

class DefaultCreateChannelComponent(
    val ctx: ComponentContext,
    val client: Client,
    val guildId: Snowflake,
    val onCreated: (Channel) -> Unit,
    val onCancel: () -> Unit
) : CreateChannelComponent {
    override val data = MutableValue(CreateChannelComponent.State())

    private val scope = ctx.coroutineScope()

    override fun onCreateChannelClicked() {
        scope.launch {
            data.value = data.value.copy(isLoading = true)
            val channel = client.createChannel(guildId, data.value.channelName)
            data.value = data.value.copy(isLoading = false)
            onCreated(channel)
        }
    }

    override fun onChannelNameChanged(channelName: String) {
        data.value = data.value.copy(
            channelName = channelName, isCreateButtonEnabled = channelName.isNotBlank()
        )
    }

    override fun onBackClicked() {
        onCancel()
    }
}
