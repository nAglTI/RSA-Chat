package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.hypergonial.chat.view.content.FallbackContent

/** The main content component
 *
 * This is the main content displayed in the app, after the user is logged in.
 * */
interface MainContentComponent

/** Appears when the user navigates to a guild that has no channels.
 *
 * This is a fallback screen that allows the user to create a channel.
 * */
interface FallbackMainComponent : MainContentComponent, Displayable {
    /** Called when the user clicks the create channel button */
    fun onChannelCreateClicked()

    @Composable
    override fun Display() = FallbackContent(this)
}

/** The default implementation of the fallback main component
 *
 * @param ctx The component context
 * @param onChannelCreateRequest The callback to call when the user requests to create a channel
 * */
class DefaultFallbackMainComponent(
    val ctx: ComponentContext,
    val onChannelCreateRequest: () -> Unit
) : FallbackMainComponent, ComponentContext by ctx {
    override fun onChannelCreateClicked() {
        onChannelCreateRequest()
    }
}
