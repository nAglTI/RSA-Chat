package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext

interface MainContentComponent

/** Appears when the user navigates to a guild that has no channels. */
interface FallbackMainComponent : MainContentComponent {
    fun onChannelCreateClicked()
}

class DefaultFallbackMainComponent(
    val ctx: ComponentContext,
    val onChannelCreateRequest: () -> Unit
) : FallbackMainComponent, ComponentContext by ctx {
    override fun onChannelCreateClicked() {
        onChannelCreateRequest()
    }
}
