package com.hypergonial.chat.view.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ChatBar(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTextChange: (TextFieldValue) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") },
    onSend: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val interactionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    )

    TextField(modifier = modifier.onPreviewKeyEvent {
        if (!enabled) return@onPreviewKeyEvent false

        if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
            focusManager.clearFocus()
            return@onPreviewKeyEvent true
        }

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
    }.padding(20.dp).border(
        1.dp,
        borderColor,
        RoundedCornerShape(16.dp)
    ),
        enabled = enabled,
        value = value,
        placeholder = { Text("Type a message...") },
        onValueChange = onTextChange,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            IconButton(onClick = onSend) {
                trailingIcon?.invoke()
            }
        })
}

private fun String.insert(index: Int, s: String): String =
    this.substring(0, index) + s + this.substring(index)
