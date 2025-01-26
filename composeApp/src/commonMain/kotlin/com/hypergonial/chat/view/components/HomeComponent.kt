package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.hypergonial.chat.view.content.HomeContent

interface HomeComponent : MainContentComponent, Displayable {
    @Composable
    override fun Display() = HomeContent(this)
}

class DefaultHomeComponent(
    val ctx: ComponentContext,
) : HomeComponent, ComponentContext by ctx
