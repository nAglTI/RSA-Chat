package com.hypergonial.chat.view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.hypergonial.chat.platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger

class AnimatedDialogScope(
    private val coroutineScope: CoroutineScope,
    private val onDismissFlow: MutableSharedFlow<Any>,
) {

    /** When invoked, it plays the exit animation and dismisses the dialog */
    fun dismissWithAnimation() {
        coroutineScope.launch { onDismissFlow.emit(Any()) }
    }
}

/**
 * Properties for the animated dialog
 *
 * @param dismissOnBackPress: If true, the dialog will be dismissed when the back button is pressed
 * @param dismissOnClickOutside: If true, the dialog will be dismissed when the user clicks outside the dialog
 */
data class AnimatedDialogProperties(val dismissOnBackPress: Boolean = true, val dismissOnClickOutside: Boolean = true) {
    fun toDialogProperties(): DialogProperties {
        return DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        )
    }

    fun toPopupProperties(): PopupProperties {
        return PopupProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            focusable = true,
        )
    }
}

/**
 * A dialog with an animated entry and exit animation
 *
 * @param onDismissRequest: A callback that is invoked when the dialog is dismissed
 * @param enter: The enter animation
 * @param exit: The exit animation
 * @param needsScrim: If true, a scrim will be displayed behind the dialog
 * @param contentAlignment: The alignment of the dialog content
 * @param content: The content of the dialog
 */
@Composable
fun AnimatedDialog(
    onDismissRequest: () -> Unit,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    needsScrim: Boolean = true,
    properties: AnimatedDialogProperties? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable AnimatedDialogScope.() -> Unit,
) {
    // The popup implementation doesn't apply scrim on mobile properly
    // But the Dialog impl scrim doesn't have an animated entry/exit
    if (platform.isMobile() && needsScrim) {
        DialogImpl(
            onDismissRequest,
            enter,
            exit,
            contentAlignment,
            (properties ?: AnimatedDialogProperties()).toDialogProperties(),
            content,
        )
    } else {
        PopupImpl(
            onDismissRequest,
            enter,
            exit,
            needsScrim,
            contentAlignment,
            (properties ?: AnimatedDialogProperties()).toPopupProperties(),
            content,
        )
    }
}

@Composable
private fun DialogImpl(
    onDismissRequest: () -> Unit,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    contentAlignment: Alignment = Alignment.Center,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable AnimatedDialogScope.() -> Unit,
) {
    val onDismissFlow: MutableSharedFlow<Any> = remember { MutableSharedFlow() }
    val scope = rememberCoroutineScope()
    // If true, the dialog content is animated in
    var isVisible by remember { mutableStateOf(false) }
    // If true, the popup has been inflated, and the entry anim can safely begin
    var dialogInflated by remember { mutableStateOf(false) }

    val onDismissAnimated = {
        scope.launch {
            isVisible = false
            dialogInflated = false
            delay(200)
            onDismissRequest()
        }
    }

    LaunchedEffect(dialogInflated) {
        if (!dialogInflated) return@LaunchedEffect
        isVisible = true
        launch { onDismissFlow.collectLatest { onDismissAnimated() } }
    }

    Dialog(onDismissRequest = { onDismissAnimated() }, properties = properties) {
        // Indicate that the popup has been inflated
        LaunchedEffect(Unit) { dialogInflated = true }

        Box(modifier = Modifier.fillMaxSize()) { // This outer box is needed otherwise the open anim is very wonky
            AnimatedVisibility(visible = isVisible, enter = enter, exit = exit, modifier = Modifier.fillMaxSize()) {
                val dialogScope = AnimatedDialogScope(scope, onDismissFlow)

                Box(
                    contentAlignment = contentAlignment,
                    modifier =
                        Modifier.fillMaxSize().let {
                            if (properties.dismissOnClickOutside) {
                                it.clickable(null, null) { dialogScope.dismissWithAnimation() }
                            } else it
                        },
                ) {
                    Box(Modifier.clickable(null, null) { /* Ignore clicks on the content */}) {
                        content(dialogScope)
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupImpl(
    onDismissRequest: () -> Unit,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    needsScrim: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    properties: PopupProperties = PopupProperties(dismissOnClickOutside = false, focusable = true),
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

    Popup(onDismissRequest = { onDismissAnimated() }, properties = properties) {
        // Indicate that the popup has been inflated
        LaunchedEffect(Unit) { popupInflated = true }

        Box(modifier = Modifier.fillMaxSize()) {
            if (needsScrim) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
                }
            }

            AnimatedVisibility(visible = isVisible, enter = enter, exit = exit, modifier = Modifier.fillMaxSize()) {
                val dialogScope = AnimatedDialogScope(scope, onDismissFlow)

                Box(
                    contentAlignment = contentAlignment,
                    modifier =
                        Modifier.fillMaxSize().let {
                            if (properties.dismissOnClickOutside) {
                                it.clickable(null, null) { dialogScope.dismissWithAnimation() }
                            } else it
                        },
                ) {
                    Box(Modifier.clickable(null, null) { /* Ignore clicks on the content */}) {
                        content(dialogScope)
                    }
                }
            }
        }
    }
}

/**
 * Popup implementation that exposes the additional fields that Jetbrains has added to their Popup implementation
 *
 * @param alignment: The alignment of the popup
 * @param offset: The offset of the popup
 * @param onDismissRequest: A callback that is invoked when the popup is dismissed
 * @param onPreviewKeyEvent: A callback that is invoked when a key event is previewed This field is ignored on Android
 *   because the default Popup implementation doesn't implement it
 * @param onKeyEvent: A callback that is invoked when a key event is received This field is ignored on Android because
 *   the default Popup implementation doesn't implement it
 * @param properties: The properties of the popup
 * @param content: The content of the popup
 */
@Composable
expect fun JetbrainsPopup(
    onDismissRequest: () -> Unit,
    alignment: Alignment = Alignment.Center,
    offset: IntOffset = IntOffset.Zero,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable () -> Unit,
)
