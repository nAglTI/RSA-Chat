package com.hypergonial.chat.view.components.prompts

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.components.Displayable
import com.hypergonial.chat.view.content.prompts.JoinGuildContent
import kotlinx.coroutines.launch

interface JoinGuildComponent: Displayable {
    fun onGuildJoinClicked()

    fun onBackClicked()

    fun onInviteCodeChanged(inviteCode: String)

    @Composable
    override fun Display() = JoinGuildContent(this)

    val data: Value<State>

    data class State(
        val inviteCode: String = "",
        val isJoinButtonEnabled: Boolean = false,
        val isLoading: Boolean = false
    )
}

class DefaultJoinGuildComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onJoined: (Member) -> Unit,
    val onCancel: () -> Unit
) : JoinGuildComponent {
    override val data = MutableValue(JoinGuildComponent.State())
    private val scope = ctx.coroutineScope()

    override fun onGuildJoinClicked() {
        val guildId = Snowflake(data.value.inviteCode.toULongOrNull() ?: return)

        scope.launch {
            data.value = data.value.copy(isLoading = true)
            val member = client.joinGuild(guildId)
            data.value = data.value.copy(isLoading = false)
            onJoined(member)
        }
    }

    override fun onInviteCodeChanged(inviteCode: String) {
        data.value = data.value.copy(
            inviteCode = inviteCode,
            isJoinButtonEnabled = inviteCode.isNotBlank() && inviteCode.toULongOrNull() != null
        )
    }

    override fun onBackClicked() {
        onCancel()
    }
}
