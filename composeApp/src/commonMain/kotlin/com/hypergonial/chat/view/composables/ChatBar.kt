package com.hypergonial.chat.view.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun ChatBar(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTextChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit
) {
    TextField(modifier = modifier.onPreviewKeyEvent {
        if (!enabled) return@onPreviewKeyEvent false

        if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
            if (it.isShiftPressed) {
                // Insert a newline at cursor position
                val cursorPosition = value.selection.start
                val newText = value.text.insert(cursorPosition, "\n")
                val newSelection = TextRange(cursorPosition + 1, cursorPosition + 1)
                onTextChange(value.copy(text = newText, selection = newSelection))
            } else if (value.text.isNotBlank()) {
                onSend()
            }
            return@onPreviewKeyEvent true
        }
        false
    },
        enabled = enabled,
        value = value,
        onValueChange = onTextChange,
        trailingIcon = {
        IconButton(onClick = onSend) {
            Icon(Icons.Filled.Done, contentDescription = "Send")
        }
    })
}

private fun String.insert(index: Int, s: String): String = this.substring(0, index) + s + this.substring(index)
