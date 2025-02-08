package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.view.content.UserSettingsContent
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

interface UserSettingsComponent : Displayable {
    val data: Value<UserSettingsState>

    @Composable override fun Display() = UserSettingsContent(this)

    fun onUsernameChange(username: String)

    fun onDisplayNameChange(displayName: String)

    fun onSaveClicked()

    fun onAvatarChangeRequested()

    data class UserSettingsState(
        val username: String = "",
        val displayName: String = "",
        val avatarUrl: String? = null,
    )
}

class DefaultUserSettingsComponent(private val ctx: ComponentContext, private val client: Client) :
    UserSettingsComponent, ComponentContext by ctx {
    override val data =
        MutableValue(
            UserSettingsComponent.UserSettingsState(
                client.cache.ownUser?.username ?: "",
                client.cache.ownUser?.displayName ?: "",
            )
        )
    private val scope = ctx.coroutineScope()

    override fun onUsernameChange(username: String) {
        data.value = data.value.copy(username = username)
    }

    override fun onDisplayNameChange(displayName: String) {
        data.value = data.value.copy(displayName = displayName)
    }

    override fun onSaveClicked() {
        scope.launch { client.updateUser(data.value.username, data.value.displayName) }
    }

    override fun onAvatarChangeRequested() {
        // Use cached data instead of state to prevent accidentally updating the display/username
        val ownUser = client.cache.ownUser ?: return

        scope.launch {
            val file =
                FileKit.pickFile(PickerType.Image, PickerMode.Single, title = "Select a new avatar") ?: return@launch
            // TODO: Somehow crop the image to a square? No image manipulation libs though :(
            client.updateUser(ownUser.username, ownUser.displayName, file)
        }
    }
}
