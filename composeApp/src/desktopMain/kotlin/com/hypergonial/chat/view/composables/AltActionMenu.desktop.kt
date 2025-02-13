package com.hypergonial.chat.view.composables

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

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
    val actions = AltMenuScope().apply(altActions).allItems
    val menuItems =
        remember(altActions) {
            actions.filter { it.showOnDesktop }.map { ContextMenuItemWithIcon(it.text, it.leadingIcon, it.onClick) }
        }

    ContextMenuDataProvider({ menuItems }) {
        ContextMenuArea({
            // Empty because otherwise it would duplicate the options
            emptyList()
        }) {
            content()
        }
    }
}
