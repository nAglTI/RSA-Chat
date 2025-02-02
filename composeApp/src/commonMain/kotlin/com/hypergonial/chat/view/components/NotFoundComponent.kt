package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.hypergonial.chat.view.content.NotFoundContent

/** The not found component
 *
 * This is displayed when the user navigates to a page that does not exist
 * */
interface NotFoundComponent : Displayable {
    @Composable
    override fun Display() = NotFoundContent(this)
}

/** The default implementation of the not found component
 *
 * @param ctx The component context
 * */
class DefaultNotFoundComponent(val ctx: ComponentContext) : NotFoundComponent
