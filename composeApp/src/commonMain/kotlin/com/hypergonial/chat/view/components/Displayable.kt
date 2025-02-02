package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable

/** A component that has a corresponding composable function to render its content. */
interface Displayable {
    /** The composable function that renders the contents of this component. */
    @Composable fun Display()
}
