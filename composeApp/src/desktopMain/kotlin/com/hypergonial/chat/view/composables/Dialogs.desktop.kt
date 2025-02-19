package com.hypergonial.chat.view.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
actual fun JetbrainsPopup(
    onDismissRequest: () -> Unit,
    alignment: Alignment,
    offset: IntOffset,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
    onKeyEvent: ((KeyEvent) -> Boolean)?,
    properties: PopupProperties,
    content: @Composable () -> Unit,
) {
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismissRequest,
        properties = properties,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content,
    )
}
