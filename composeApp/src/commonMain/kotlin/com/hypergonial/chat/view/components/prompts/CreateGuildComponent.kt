package com.hypergonial.chat.view.components.prompts

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.SnackbarContainer
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.exceptions.ApiException
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.view.components.Displayable
import com.hypergonial.chat.view.content.prompts.CreateGuildContent
import io.github.oshai.kotlinlogging.KotlinLogging
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
        val snackbarMessage: SnackbarContainer<String> = SnackbarContainer(""),
    )
}

class DefaultCreateGuildComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onCreated: (Guild) -> Unit,
    val onCancel: () -> Unit,
) : CreateGuildComponent {
    private val scope = ctx.coroutineScope()
    private val logger = KotlinLogging.logger {}

    override val data = MutableValue(CreateGuildComponent.State())

    override fun onGuildCreateClicked() {
        scope.launch {
            data.value = data.value.copy(isLoading = true)
            val guild =
                try {
                    client.createGuild(data.value.guildName.trim())
                } catch (e: ApiException) {
                    logger.error { "Failed to create guild: ${e.message}" }
                    data.value =
                        data.value.copy(
                            isLoading = false,
                            snackbarMessage =
                                SnackbarContainer(e.message ?: "An error occurred, please try again later."),
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
                isCreateButtonEnabled = guildName.isNotBlank(),
            )
    }

    override fun onBackClicked() {
        onCancel()
    }
}
