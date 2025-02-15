package com.hypergonial.chat.view.components.prompts

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.hypergonial.chat.view.components.Displayable
import com.hypergonial.chat.view.content.prompts.NewGuildContent

interface NewGuildComponent : Displayable {
    /** Called when the create button is clicked */
    fun onGuildCreateClicked()

    /** Called when the join button is clicked */
    fun onGuildJoinClicked()

    /** Called when the back button is clicked */
    fun onBackClicked()

    @Composable override fun Display() = NewGuildContent(this)
}

class DefaultNewGuildComponent(
    val ctx: ComponentContext,
    val onCreateRequested: () -> Unit,
    val onJoinRequested: () -> Unit,
    val onCancel: () -> Unit,
) : NewGuildComponent {
    override fun onGuildCreateClicked() = onCreateRequested()

    override fun onGuildJoinClicked() = onJoinRequested()

    override fun onBackClicked() = onCancel()
}
