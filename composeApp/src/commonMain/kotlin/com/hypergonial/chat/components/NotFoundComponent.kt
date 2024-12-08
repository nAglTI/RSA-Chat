package com.hypergonial.chat.components

import com.arkivanov.decompose.ComponentContext

interface NotFoundComponent

class DefaultNotFoundComponent(val ctx: ComponentContext) : NotFoundComponent
