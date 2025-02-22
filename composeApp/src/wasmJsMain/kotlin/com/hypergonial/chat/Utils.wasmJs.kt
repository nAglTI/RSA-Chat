package com.hypergonial.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.altClickable(onClick: () -> Unit): Modifier {
    return pointerInput(Unit) {
            coroutineScope {
                awaitEachGesture {
                    // Wait for the first down event
                    awaitFirstDown(requireUnconsumed = false)
                    // Launch a coroutine that will invoke onLongTap if the press lasts long enough
                    val longPressJob = launch {
                        delay(viewConfiguration.longPressTimeoutMillis) // long press threshold in milliseconds
                        onClick()
                    }
                    // Wait until the pointer is released or cancelled
                    var pointerReleased = false
                    while (!pointerReleased) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        // If all pointers are up, finish the gesture.
                        if (event.changes.all { !it.pressed }) {
                            pointerReleased = true
                            // Cancel the long press callback if the pointer was released too soon.
                            longPressJob.cancel()
                        } else {
                            // Consume the event to avoid interference with other gestures.
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
        .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary), onClick = { onClick() })
}
