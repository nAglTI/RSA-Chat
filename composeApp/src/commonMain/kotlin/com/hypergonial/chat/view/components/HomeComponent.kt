package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.hypergonial.chat.view.content.HomeContent

/**
 * The home component
 *
 * This is displayed when the user has no guilds selected
 */
interface HomeComponent : MainContentComponent, Displayable {
    @Composable override fun Display() = HomeContent(this)
}

/**
 * The default implementation of the home component
 *
 * @param ctx The component context
 */
class DefaultHomeComponent(val ctx: ComponentContext) : HomeComponent, ComponentContext by ctx
