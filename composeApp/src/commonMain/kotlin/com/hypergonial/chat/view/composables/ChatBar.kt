package com.hypergonial.chat.view.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hypergonial.chat.isPasteGesture
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.editorFocusInhibitor
import com.hypergonial.chat.view.globalKeyEventFlow
import kotlinx.coroutines.launch

private val invalidCharCategories =
    listOf(
        CharCategory.CONTROL,
        CharCategory.UNASSIGNED,
        CharCategory.LINE_SEPARATOR,
        CharCategory.PARAGRAPH_SEPARATOR,
        CharCategory.FORMAT,
        CharCategory.PRIVATE_USE,
        CharCategory.SURROGATE,
    )

/**
 * A chat bar that allows the user to type messages and send them. It supports multi-line input, sending messages on
 * enter, and dropping focus on escape.
 *
 * @param value The current value of the chat bar
 * @param modifier The modifier to apply to the chat bar
 * @param editorKey The key to use for the editor focus inhibitor
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
    editorKey: String,
    enabled: Boolean = true,
    shouldGrabFocus: Boolean = false,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusGain: (() -> Unit)? = null,
    onFocusLoss: (() -> Unit)? = null,
    onEditLastRequested: (() -> Unit)? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingButtonEnabled: Boolean = true,
    leadingButtonEnabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            modifier = Modifier.pointerHoverIcon(if (trailingButtonEnabled) PointerIcon.Hand else PointerIcon.Default),
        )
    },
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var hasGrabbedFocus by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    EditorFocusInhibitor(editorKey, isFocused)

    DisposableEffect(value.text, onValueChange) {
        val receiveGlobalKeys =
            if (editorKey == "MAIN_EDITOR" && platform.isDesktopOrWeb()) {
                scope.launch {
                    globalKeyEventFlow.receive { event ->
                        if (isFocused || event.type != KeyEventType.KeyDown) {
                            return@receive
                        }

                        if (event.isPasteGesture()) {
                            val newText = value.text + clipboardManager.getText()?.text
                            focusRequester.requestFocus()
                            onValueChange(
                                value.copy(
                                    text = newText,
                                    selection = TextRange(start = newText.length, end = newText.length),
                                )
                            )
                            return@receive
                        }

                        val char = event.utf16CodePoint.toChar()

                        if (
                            event.isMetaPressed ||
                                event.isAltPressed ||
                                event.isCtrlPressed ||
                                char.category in invalidCharCategories
                        ) {
                            return@receive
                        }

                        scope.launch {
                            if (editorFocusInhibitor.isFree()) {
                                focusRequester.requestFocus()
                                onValueChange(
                                    value.copy(
                                        text = value.text + char,
                                        selection =
                                            TextRange(start = value.text.length + 1, end = value.text.length + 1),
                                    )
                                )
                            }
                        }
                    }
                }
            } else null

        onDispose { receiveGlobalKeys?.cancel() }
    }

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
                        } else {
                            onSubmit()
                        }
                        return@onPreviewKeyEvent true
                    }
                    false
                }
                .focusRequester(focusRequester)
                .shadow(8.dp, RoundedCornerShape(16.dp), clip = false),
        enabled = enabled,
        value = value,
        placeholder = { Text("Type a message...") },
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(350)),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        leadingIcon =
            if (leadingIcon != null) {
                {
                    IconButton(
                        onClick = { onLeadingIconClick?.invoke() },
                        modifier = Modifier.focusProperties { canFocus = false },
                        enabled = leadingButtonEnabled,
                    ) {
                        leadingIcon()
                    }
                }
            } else null,
        trailingIcon =
            if (trailingIcon != null) {
                {
                    IconButton(
                        onClick = onSubmit,
                        modifier = Modifier.focusProperties { canFocus = false },
                        enabled = trailingButtonEnabled,
                    ) {
                        trailingIcon()
                    }
                }
            } else null,
    )
}

private fun String.insert(index: Int, s: String): String = this.substring(0, index) + s + this.substring(index)
