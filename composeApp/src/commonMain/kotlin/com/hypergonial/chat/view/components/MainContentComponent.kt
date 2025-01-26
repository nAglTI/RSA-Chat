package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.hypergonial.chat.view.content.FallbackContent

interface MainContentComponent

/** Appears when the user navigates to a guild that has no channels. */
interface FallbackMainComponent : MainContentComponent, Displayable {
    fun onChannelCreateClicked()

    @Composable
    override fun Display() = FallbackContent(this)
}

class DefaultFallbackMainComponent(
    val ctx: ComponentContext,
    val onChannelCreateRequest: () -> Unit
) : FallbackMainComponent, ComponentContext by ctx {
    override fun onChannelCreateClicked() {
        onChannelCreateRequest()
    }
}
