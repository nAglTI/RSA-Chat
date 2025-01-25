package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext

interface HomeComponent : MainContentComponent

class DefaultHomeComponent(
    val ctx: ComponentContext,
) : HomeComponent, ComponentContext by ctx
