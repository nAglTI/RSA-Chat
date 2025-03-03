package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.EffectContainer
import com.hypergonial.chat.containAsEffect
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.GuildRemoveEvent
import com.hypergonial.chat.model.GuildUpdateEvent
import com.hypergonial.chat.model.LifecyclePausedEvent
import com.hypergonial.chat.model.exceptions.ClientException
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.content.GuildSettingsContent
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

interface GuildSettingsComponent : Displayable {
    val data: Value<State>

    @Composable override fun Display() = GuildSettingsContent(this)

    /** Invoked when the user changes the guild's name in the text field */
    fun onNameChange(name: String)

    /** Invoked when the user clicks the save button */
    fun onSaveClicked()

    /** Invoked when the user clicks on the avatar */
    fun onAvatarChangeRequested()

    /** Invoked when the user clicks the remove avatar button */
    fun onAvatarRemoveRequested()

    /** Invoked when the user clicks the back button */
    fun onBackClicked()

    data class State(
        val guildName: String = "",
        val avatarUrl: String? = null,
        val canSave: Boolean = false,
        val guildNameErrors: List<String> = emptyList(),
        val snackbarMessage: EffectContainer<String> = "".containAsEffect(),
    )
}

class DefaultGuildSettingsComponent(
    private val ctx: ComponentContext,
    private val client: Client,
    private val guildId: Snowflake,
    private val onBack: () -> Unit,
) : GuildSettingsComponent, ComponentContext by ctx {
    override val data =
        MutableValue(
            GuildSettingsComponent.State(
                client.cache.getGuild(guildId)?.name ?: "",
                client.cache.getGuild(guildId)?.avatarUrl,
            )
        )
    private val scope = ctx.coroutineScope()
    private val logger = Logger.withTag("DefaultGuildSettingsComponent")

    init {
        client.eventManager.apply {
            subscribeWithLifeCycle(ctx.lifecycle, ::onGuildUpdate)
            subscribeWithLifeCycle(ctx.lifecycle, ::onGuildRemove)
            subscribeWithLifeCycle(ctx.lifecycle, ::onLifecyclePausedEvent)
        }
    }

    private fun updateCanSave() {
        data.value =
            data.value.copy(canSave = data.value.guildNameErrors.isEmpty() && data.value.guildName.isNotEmpty())
    }

    private fun getGuildNameErrors(guildName: String): List<String> {
        if (guildName.isEmpty()) {
            return emptyList()
        }

        if (guildName.isBlank()) {
            return listOf("Guild name must not be blank")
        }

        if (guildName.length < 3) {
            return listOf("Guild name must be at least 3 characters")
        }

        if (guildName.length > 32) {
            return listOf("Guild name must be at most 32 characters")
        }

        return emptyList()
    }

    override fun onNameChange(name: String) {
        data.value = data.value.copy(guildName = name, guildNameErrors = getGuildNameErrors(name))
        updateCanSave()
    }

    private fun onGuildUpdate(event: GuildUpdateEvent) {
        if (event.guild.id != guildId) return

        data.value =
            data.value.copy(guildName = event.guild.name, avatarUrl = event.guild.avatarUrl ?: data.value.avatarUrl)
    }

    private fun onGuildRemove(event: GuildRemoveEvent) {
        if (event.guild.id != guildId) return

        onBack()
    }

    @Suppress("UnusedParameter")
    private fun onLifecyclePausedEvent(event: LifecyclePausedEvent) {
        onBack()
    }

    override fun onSaveClicked() {
        data.value = data.value.copy(canSave = false)
        scope.launch {
            try {
                client.updateGuild(guildId) { name = data.value.guildName }
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(
                        snackbarMessage = "Failed to update guild settings, please try again later.".containAsEffect()
                    )
                logger.e { "Failed to update guild settings: ${e.message}" }
                return@launch
            }

            data.value = data.value.copy(snackbarMessage = "Guild settings updated".containAsEffect())
        }
    }

    override fun onBackClicked() = onBack()

    override fun onAvatarChangeRequested() {
        scope.launch {
            val file =
                FileKit.pickFile(PickerType.Image, PickerMode.Single, title = "Select a new guild icon")
                    ?: return@launch

            file.getSize()?.let {
                if (it > 2 * 1024 * 1024) {
                    data.value =
                        data.value.copy(snackbarMessage = "Guild icon size must be less than 2MB".containAsEffect())
                    return@launch
                }
            }

            try {
                client.updateGuild(guildId) { avatar = file }
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(
                        snackbarMessage = "Failed to update guild icon, please try again later.".containAsEffect()
                    )
                logger.e { "Failed to update guild icon: ${e.message}" }
                return@launch
            }

            data.value = data.value.copy(snackbarMessage = "Icon updated".containAsEffect())
        }
    }

    override fun onAvatarRemoveRequested() {
        scope.launch {
            try {
                client.updateGuild(guildId) { avatar = null }
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(
                        snackbarMessage = "Failed to remove icon, please try again later.".containAsEffect()
                    )
                logger.e { "Failed to remove icon: ${e.message}" }
                return@launch
            }
            data.value = data.value.copy(snackbarMessage = "Icon removed".containAsEffect())
        }
    }
}
