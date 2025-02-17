package com.hypergonial.chat.view.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.hypergonial.chat.view.editorFocusInhibitor

/**
 * When this composable enters the composition, if enabled, it acquires the editor focus inhibitor with the given [key].
 * When this composable leaves the composition, it releases the editor focus inhibitor.
 */
@Composable
fun EditorFocusInhibitor(key: String, isEnabled: Boolean = true) {
    DisposableEffect(isEnabled) {
        if (isEnabled) {
            editorFocusInhibitor.acquire(key)
        }

        onDispose { editorFocusInhibitor.release(key) }
    }
}
