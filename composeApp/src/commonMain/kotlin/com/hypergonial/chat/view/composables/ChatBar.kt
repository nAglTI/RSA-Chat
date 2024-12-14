package com.hypergonial.chat.view.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
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
                    .removeRange(value.selection.start until value.selection.end)
                //val newSelection = TextRange(cursorPosition + 1, cursorPosition + 1)
                onTextChange(value.copy(text = newText))
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

private fun String.insert(index: Int, s: String): String {
    return this.substring(0, index) + s + this.substring(index)
}

private fun String.removeRange(range: IntRange): String {
    if (range.first == -1 || range.last == -1) return this
    if (range.first == range.last) return this

    return this.substring(0, range.first) + this.substring(range.last)
}
