package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.view.content.HomeContent
import kotlinx.coroutines.launch

/**
 * The home component
 *
 * This is displayed when the user has no guilds selected
 */
interface HomeComponent : MainContentComponent, Displayable {
    @Composable override fun Display() = HomeContent(this)

    val data: Value<State>

    data class State(val hasGuilds: Boolean, val isLoading: Boolean = true)
}

/**
 * The default implementation of the home component
 *
 * @param ctx The component context
 */
class DefaultHomeComponent(val ctx: ComponentContext, private val client: Client, hasGuilds: Boolean) :
    HomeComponent, ComponentContext by ctx {
    override val data = MutableValue(HomeComponent.State(hasGuilds, isLoading = !client.isReady()))
    private val scope = ctx.coroutineScope()

    init {
        scope.launch {
            client.waitUntilReady()
            data.value = data.value.copy(isLoading = false)
        }
    }
}
