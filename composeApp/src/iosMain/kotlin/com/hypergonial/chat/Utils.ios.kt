package com.hypergonial.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid

@Composable
actual fun Modifier.altClickable(onClick: () -> Unit): Modifier {
    return pointerInput(Unit) {
        coroutineScope {
            awaitEachGesture {
                // Wait for the first down event
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val initialPosition = firstDown.position
                // Launch a coroutine that will invoke onLongTap if the press lasts long enough
                val longPressJob = launch {
                    delay(viewConfiguration.longPressTimeoutMillis)
                    UIImpactFeedbackGenerator(style = UIImpactFeedbackStyleRigid).apply {
                        prepare()
                        impactOccurred()
                    }
                    onClick()
                }
                // Wait until the pointer is released or cancelled
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Main)

                    // If pointer moves too far away from the initial position, cancel the long press.
                    if (
                        event.changes.any {
                            (it.position - initialPosition).getDistance() > viewConfiguration.touchSlop
                        }
                    ) {
                        longPressJob.cancel()
                        break
                    }

                    // If all pointers are up, finish the gesture.
                    if (event.changes.all { !it.pressed }) {
                        longPressJob.cancel()
                        break
                    }
                }
            }
        }
    }
}
