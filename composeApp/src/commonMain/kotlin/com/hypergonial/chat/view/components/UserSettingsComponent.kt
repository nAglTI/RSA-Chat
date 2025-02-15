package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.EffectContainer
import com.hypergonial.chat.containAsEffect
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.UserUpdateEvent
import com.hypergonial.chat.model.exceptions.ClientException
import com.hypergonial.chat.view.content.UserSettingsContent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

interface UserSettingsComponent : Displayable {
    val data: Value<UserSettingsState>

    @Composable override fun Display() = UserSettingsContent(this)

    /** Invoked when the user changes their username */
    fun onUsernameChange(username: String)

    /** Invoked when the user changes their display name */
    fun onDisplayNameChange(displayName: String)

    /** Invoked when the user clicks the save button */
    fun onSaveClicked()

    /** Invoked when the user clicks on the avatar */
    fun onAvatarChangeRequested()

    /** Invoked when the user clicks the back button */
    fun onBackClicked()

    data class UserSettingsState(
        val username: String = "",
        val displayName: String = "",
        val avatarUrl: String? = null,
        val canSave: Boolean = false,
        val usernameErrors: List<String> = emptyList(),
        val displayNameErrors: List<String> = emptyList(),
        val snackbarMessage: EffectContainer<String> = "".containAsEffect(),
    )
}

class DefaultUserSettingsComponent(
    private val ctx: ComponentContext,
    private val client: Client,
    private val onBack: () -> Unit,
) : UserSettingsComponent, ComponentContext by ctx {
    override val data =
        MutableValue(
            UserSettingsComponent.UserSettingsState(
                client.cache.ownUser?.username ?: "",
                client.cache.ownUser?.displayName ?: "",
                client.cache.ownUser?.avatarUrl,
            )
        )
    private val scope = ctx.coroutineScope()
    private val usernameRegex = Regex("^([a-z0-9]|[a-z0-9]+(?:[._][a-z0-9]+)*)\$")
    private val logger = KotlinLogging.logger {}

    init {
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onUserUpdate)
    }

    private fun updateCanSave() {
        data.value =
            data.value.copy(canSave = data.value.usernameErrors.isEmpty() && data.value.displayNameErrors.isEmpty())
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

    private fun getDisplayNameErrors(displayName: String): List<String> {
        if (displayName.isEmpty()) {
            return emptyList()
        }

        if (displayName.isBlank()) {
            return listOf("Display name must not be blank")
        }

        if (displayName.length < 3) {
            return listOf("Display name must be at least 3 characters")
        }

        if (displayName.length > 32) {
            return listOf("Display name must be at most 32 characters")
        }

        return emptyList()
    }

    override fun onUsernameChange(username: String) {
        data.value = data.value.copy(username = username, usernameErrors = getUsernameErrors(username))
        updateCanSave()
    }

    override fun onDisplayNameChange(displayName: String) {
        data.value = data.value.copy(displayName = displayName, displayNameErrors = getDisplayNameErrors(displayName))
        updateCanSave()
    }

    private fun onUserUpdate(event: UserUpdateEvent) {
        if (event.user.id != client.cache.ownUser?.id) return

        data.value =
            data.value.copy(
                username = event.user.username,
                displayName = event.user.displayName ?: "",
                avatarUrl = event.user.avatarUrl,
            )
    }

    override fun onSaveClicked() {
        data.value = data.value.copy(canSave = false)
        scope.launch {
            try {
                client.updateSelf(data.value.username, data.value.displayName.ifEmpty { null })
            }
            catch (e: ClientException) {
                data.value =
                    data.value.copy(
                        snackbarMessage = "Failed to update user settings, please try again later.".containAsEffect()
                    )
                logger.error { "Failed to update user settings: ${e.message}" }
                return@launch
            }

            data.value = data.value.copy(snackbarMessage = "User settings updated".containAsEffect())
        }
    }

    override fun onBackClicked() = onBack()

    override fun onAvatarChangeRequested() {
        // Use cached data instead of state to prevent accidentally updating the display/username
        val ownUser = client.cache.ownUser ?: return

        scope.launch {
            val file =
                FileKit.pickFile(PickerType.Image, PickerMode.Single, title = "Select a new avatar") ?: return@launch

            file.getSize()?.let {
                if (it > 2 * 1024 * 1024) {
                    data.value =
                        data.value.copy(snackbarMessage = "Avatar size must be less than 2MB".containAsEffect())
                    return@launch
                }
            }

            // TODO: Somehow crop the image to a square? No image manipulation libs though :(
            try {
                client.updateSelf(ownUser.username, ownUser.displayName, file)
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(
                        snackbarMessage = "Failed to update avatar, please try again later.".containAsEffect()
                    )
                logger.error { "Failed to update avatar: ${e.message}" }
                return@launch
            }

            data.value = data.value.copy(snackbarMessage = "Avatar updated".containAsEffect())
        }
    }
}
