package com.hypergonial.chat.view.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.hypergonial.chat.view.editorFocusInhibitor
import kotlinx.coroutines.launch

/**
 * When this composable enters the composition, if enabled, it acquires the editor focus inhibitor with the given [key].
 * When this composable leaves the composition, it releases the editor focus inhibitor.
 */
@Composable
fun EditorFocusInhibitor(key: String, isEnabled: Boolean = true) {
    val scope = rememberCoroutineScope()

    DisposableEffect(isEnabled) {
        if (isEnabled) {
            scope.launch { editorFocusInhibitor.acquire(key) }
        }

        onDispose { scope.launch { editorFocusInhibitor.release(key) } }
    }
}
