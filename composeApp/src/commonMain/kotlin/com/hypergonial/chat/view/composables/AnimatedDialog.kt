package com.hypergonial.chat.view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnimatedDialogScope(
    private val coroutineScope: CoroutineScope,
    private val onDismissFlow: MutableSharedFlow<Any>,
) {

    /** When invoked, it plays the exit animation and dismisses the dialog */
    fun dismissWithAnimation() {
        coroutineScope.launch { onDismissFlow.emit(Any()) }
    }
}

/** A dialog with an animated entry and exit animation
 *
 * @param onDismissRequest: A callback that is invoked when the dialog is dismissed
 * @param enter: The enter animation
 * @param exit: The exit animation
 * @param contentAlignment: The alignment of the dialog content
 * @param popupProperties: The properties of the underlying popup
 * @param content: The content of the dialog
 * */
@Composable
fun AnimatedDialog(
    onDismissRequest: () -> Unit,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    contentAlignment: Alignment = Alignment.Center,
    popupProperties: PopupProperties = PopupProperties(dismissOnClickOutside = false),
    content: @Composable AnimatedDialogScope.() -> Unit,
) {
    val onDismissFlow: MutableSharedFlow<Any> = remember { MutableSharedFlow() }
    val scope = rememberCoroutineScope()
    // If true, the dialog content is animated in
    var isVisible by remember { mutableStateOf(false) }
    // If true, the popup has been inflated, and the entry anim can safely begin
    var popupInflated by remember { mutableStateOf(false) }

    val onDismissAnimated = {
        scope.launch {
            isVisible = false
            popupInflated = false
            delay(200)
            onDismissRequest()
        }
    }

    LaunchedEffect(popupInflated) {
        if (!popupInflated) return@LaunchedEffect
        isVisible = true
        launch { onDismissFlow.collectLatest { onDismissAnimated() } }
    }

    Popup(onDismissRequest = { onDismissAnimated() }, properties = popupProperties) {
        // Indicate that the popup has been inflated
        LaunchedEffect(Unit) { popupInflated = true }

        Box(contentAlignment = contentAlignment, modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
            }

            AnimatedVisibility(visible = isVisible, enter = enter, exit = exit) {
                content(AnimatedDialogScope(scope, onDismissFlow))
            }
        }
    }
}
