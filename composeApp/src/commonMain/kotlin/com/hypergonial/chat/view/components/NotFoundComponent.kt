package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.hypergonial.chat.view.content.NotFoundContent

interface NotFoundComponent : Displayable {
    @Composable
    override fun Display() = NotFoundContent(this)
}

class DefaultNotFoundComponent(val ctx: ComponentContext) : NotFoundComponent
