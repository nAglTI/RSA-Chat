package com.hypergonial.chat.view.composables

import androidx.compose.runtime.Composable

/**
 * An effect for handling presses of the system back button.
 *
 * Calling this in your composable on Android adds the given lambda to the OnBackPressedDispatcher of the
 * LocalOnBackPressedDispatcherOwner.
 *
 * On other platforms, this is a no-op and onBack will never be called.
 *
 * If this is called by nested composables, if enabled, the inner most composable will consume the call to system back
 * and invoke its lambda. The call will continue to propagate up until it finds an enabled BackHandler.
 *
 * @param isEnabled if this BackHandler should be enabled
 * @param onBack the action invoked by pressing the system back
 */
@Composable expect fun BackHandler(isEnabled: Boolean, onBack: () -> Unit)
