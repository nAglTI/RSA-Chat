package com.hypergonial.chat.view.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * A chat bar that allows the user to type messages and send them. It supports multi-line input, sending messages on
 * enter, and dropping focus on escape.
 *
 * @param value The current value of the chat bar
 * @param modifier The modifier to apply to the chat bar
 * @param enabled Whether the chat bar is enabled
 * @param shouldGrabFocus Whether the chat bar should grab focus when it is first displayed
 * @param onValueChange The callback to call when the value of the chat bar changes
 * @param trailingIcon The icon to display at the end of the chat bar
 * @param onFocusGain The callback to call when the chat bar gains focus, this will not be invoked when focus is
 *   initially requested due to shouldGrabFocus
 * @param onFocusLoss The callback to call when the chat bar loses focus
 * @param onSubmit The callback to call when the user hits enter or the send button
 */
@Composable
fun ChatBar(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shouldGrabFocus: Boolean = false,
    onValueChange: (TextFieldValue) -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        )
    },
    onFocusGain: (() -> Unit)? = null,
    onFocusLoss: (() -> Unit)? = null,
    onEditLastRequested: (() -> Unit)? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var hasGrabbedFocus by remember { mutableStateOf(false) }
    val borderColor by
        animateColorAsState(targetValue = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent)

    // Grab focus if requested, and signal when focus is lost
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocusGain?.invoke()
        }

        if (shouldGrabFocus && !hasGrabbedFocus) {
            focusRequester.requestFocus()
            hasGrabbedFocus = true
        } else if (!isFocused && hasGrabbedFocus) {
            onFocusLoss?.invoke()
        }
    }

    TextField(
        modifier =
            modifier
                .onPreviewKeyEvent {
                    if (!enabled) return@onPreviewKeyEvent false

                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        focusManager.clearFocus()
                        return@onPreviewKeyEvent true
                    }

                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
                        onEditLastRequested?.invoke()
                        return@onPreviewKeyEvent true
                    }

                    if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                        if (it.isShiftPressed) {
                            // Insert a newline at cursor position
                            val cursorPosition = value.selection.start
                            val newText = value.text.insert(cursorPosition, "\n")
                            val newSelection = TextRange(cursorPosition + 1, cursorPosition + 1)
                            onValueChange(value.copy(text = newText, selection = newSelection))
                        } else if (value.text.isNotBlank()) {
                            onSubmit()
                        }
                        return@onPreviewKeyEvent true
                    }
                    false
                }
                .focusRequester(focusRequester)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        enabled = enabled,
        value = value,
        placeholder = { Text("Type a message...") },
        onValueChange = onValueChange,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        leadingIcon = {
            if (leadingIcon != null && onLeadingIconClick != null) {
                IconButton(onClick = onLeadingIconClick, modifier = Modifier.focusProperties { canFocus = false }) {
                    leadingIcon()
                }
            }
        },
        trailingIcon = {
            if (trailingIcon != null) {
                IconButton(onClick = onSubmit, modifier = Modifier.focusProperties { canFocus = false }) {
                    trailingIcon()
                }
            }
        },
    )
}

private fun String.insert(index: Int, s: String): String = this.substring(0, index) + s + this.substring(index)
