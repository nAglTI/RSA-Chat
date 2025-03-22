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
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.view.components.Displayable
import com.hypergonial.chat.view.content.prompts.CreateGuildContent
import kotlinx.coroutines.launch

interface CreateGuildComponent : Displayable {
    fun onGuildCreateClicked()

    fun onBackClicked()

    fun onGuildNameChanged(guildName: String)

    @Composable override fun Display() = CreateGuildContent(this)

    val data: Value<State>

    data class State(
        val guildName: String = "",
        val isCreateButtonEnabled: Boolean = false,
        val isLoading: Boolean = false,
        val snackbarMessage: EffectContainer<String> = "".containAsEffect(),
    )
}

class DefaultCreateGuildComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onCreated: (Guild) -> Unit,
    val onCancel: () -> Unit,
) : CreateGuildComponent {
    private val scope = ctx.coroutineScope()
    private val logger = Logger.withTag("DefaultCreateGuildComponent")

    override val data = MutableValue(CreateGuildComponent.State())

    override fun onGuildCreateClicked() {
        scope.launch {
            data.value = data.value.copy(isLoading = true)
            val guild =
                try {
                    client.createGuild(data.value.guildName.trim())
                } catch (e: ClientException) {
                    logger.e { "Failed to create guild: ${e.message}" }
                    data.value =
                        data.value.copy(
                            isLoading = false,
                            snackbarMessage =
                                (e.message ?: "An error occurred, please try again later.").containAsEffect(),
                        )
                    return@launch
                }

            data.value = data.value.copy(isLoading = false)
            onCreated(guild)
        }
    }

    override fun onGuildNameChanged(guildName: String) {
        data.value =
            data.value.copy(
                guildName = guildName.replace(Regex("\\s+"), " ").take(32),
                isCreateButtonEnabled = guildName.length >= 3 && guildName.isNotBlank(),
            )
    }

    override fun onBackClicked() {
        onCancel()
    }
}
