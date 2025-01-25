package com.hypergonial.chat.view.components.prompts

import com.arkivanov.decompose.ComponentContext

interface NewGuildComponent {
    fun onGuildCreateClicked()

    fun onGuildJoinClicked()

    fun onBackClicked()
}

class DefaultNewGuildComponent(
    val ctx: ComponentContext,
    val onCreateRequested: () -> Unit,
    val onJoinRequested: () -> Unit,
    val onCancel: () -> Unit
) : NewGuildComponent {
    override fun onGuildCreateClicked() {
        onCreateRequested()
    }

    override fun onGuildJoinClicked() {
        onJoinRequested()
    }

    override fun onBackClicked() {
        onCancel()
    }
}
