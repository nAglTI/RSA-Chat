package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext

interface HomeComponent {
    val onLogout: () -> Unit
}

class DefaultHomeComponent(val ctx: ComponentContext, override val onLogout: () -> Unit) : HomeComponent
