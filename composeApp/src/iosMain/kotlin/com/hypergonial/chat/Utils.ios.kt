package com.hypergonial.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual fun Modifier.altClickable(onClick: () -> Unit): Modifier {
    return pointerInput(Unit) {
        coroutineScope {
            awaitEachGesture {
                // Wait for the first down event
                awaitFirstDown(requireUnconsumed = false)
                // Launch a coroutine that will invoke onLongTap if the press lasts long enough
                val longPressJob = launch {
                    delay(viewConfiguration.longPressTimeoutMillis)
                    onClick()
                }
                // Wait until the pointer is released or cancelled
                var pointerReleased = false
                while (!pointerReleased) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                    // If all pointers are up, finish the gesture.
                    if (event.changes.all { !it.pressed }) {
                        pointerReleased = true
                        longPressJob.cancel()
                    } else {
                        // Consume the event to avoid interference with other gestures.
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }
}
