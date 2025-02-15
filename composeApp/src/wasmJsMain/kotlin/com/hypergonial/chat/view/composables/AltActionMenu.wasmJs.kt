package com.hypergonial.chat.view.composables

import androidx.compose.runtime.Composable

/**
 * A wrapper for an action menu that can be displayed in an alternative way on smaller screens.
 *
 * On smaller screens, it shows a bottom sheet, while on larger screens, it shows a dropdown menu.
 *
 * @param isActive Whether the menu is active or not
 * @param onDismissRequest The callback to be invoked when the menu is hidden
 * @param altActions The actions to display in the menu
 * @param content The content to display
 */
@Composable
actual fun AltActionMenu(
    isActive: Boolean,
    onDismissRequest: () -> Unit,
    altActions: AltMenuScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    DefaultAltActionMenu(
        isActive = isActive,
        onDismissRequest = onDismissRequest,
        altActions = altActions,
        content = content,
    )
}
