package com.hypergonial.chat.view.composables

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.hypergonial.chat.platform

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun AltActionMenu(
    isActive: Boolean,
    onDismissRequest: () -> Unit,
    altActions: AltMenuScope.() -> Unit,
    content: @Composable () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAltActionMenu(
    isActive: Boolean,
    onDismissRequest: () -> Unit,
    altActions: AltMenuScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    if (!isActive) {
        content()
        return
    }
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }
    val shouldUseBottomSheet = isSmall && !platform.isDesktopOrWeb()
    val altMenuItems = remember(altActions) { AltMenuScope().apply(altActions).allItems }

    EditorFocusInhibitor("ALT_ACTION_MENU", isActive)

    if (shouldUseBottomSheet) {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(start = 30.dp, end = 30.dp, bottom = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            ) {
                altMenuItems
                    .map { item ->
                        item.copy(
                            onClick = {
                                item.onClick()
                                onDismissRequest()
                            }
                        )
                    }
                    .forEachIndexed { i, item ->
                        item.Display()

                        if (i < altMenuItems.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceDim)
                        }
                    }
            }
        }
        content()
    } else {
        Box {
            content()
            DropdownMenu(expanded = isActive, onDismissRequest = onDismissRequest, shape = RoundedCornerShape(12.dp)) {
                altMenuItems.forEach { item -> item.Display() }
            }
        }
    }
}

/**
 * A single item in an alt action menu.
 *
 * @param text The text to display for the item
 * @param onClick The callback to call when the item is clicked
 * @param modifier The modifier to apply to the item
 * @param leadingIcon The icon to display at the start of the item
 * @param trailingIcon The icon to display at the end of the item
 * @param enabled Whether the item is enabled
 * @param colors The colors to use for the item
 * @param contentPadding The padding to apply to the item
 * @param interactionSource The interaction source to use for the item
 */
@Composable
private fun AltAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }
    val shouldUseBottomSheet = isSmall && !platform.isDesktopOrWeb()

    if (!shouldUseBottomSheet) {
        DropdownMenuItem(
            text = { Text(text) },
            onClick = onClick,
            modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled,
            colors = colors,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
        )
    } else {
        Row(
            modifier
                .fillMaxWidth()
                .clickable(interactionSource, LocalIndication.current, enabled = enabled, onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(vertical = 15.dp, horizontal = 20.dp)
        ) {
            Box(modifier = Modifier.padding(end = 5.dp)) { leadingIcon?.invoke() }
            Text(text, softWrap = false, fontSize = 18.sp, fontStyle = MaterialTheme.typography.bodyLarge.fontStyle)
            Box(modifier = Modifier.padding(start = 5.dp)) { trailingIcon?.invoke() }
        }
    }
}

@Immutable
data class AltMenuItem(
    val text: String,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val trailingIcon: @Composable (() -> Unit)? = null,
    val enabled: Boolean = true,
    val colors: MenuItemColors? = null,
    val contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    val interactionSource: MutableInteractionSource? = null,
    val showOnDesktop: Boolean = true,
    val onClick: () -> Unit,
) {
    @Stable
    @Composable
    fun Display() {
        AltAction(
            text = text,
            onClick = onClick,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled,
            colors = colors ?: MenuDefaults.itemColors(),
            contentPadding = contentPadding,
            interactionSource = interactionSource,
        )
    }
}

class AltMenuScope {
    private val _items = mutableListOf<AltMenuItem>()

    internal val allItems: List<AltMenuItem>
        get() = _items

    /**
     * Adds a single item in an alt action menu.
     *
     * @param text The text to display for the item
     * @param onClick The callback to call when the item is clicked
     * @param leadingIcon The icon to display at the start of the item
     * @param trailingIcon The icon to display at the end of the item
     * @param enabled Whether the item is enabled
     * @param colors The colors to use for the item
     * @param contentPadding The padding to apply to the item
     * @param interactionSource The interaction source to use for the item
     */
    fun item(
        text: String,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        colors: MenuItemColors? = null,
        contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
        interactionSource: MutableInteractionSource? = null,
        showOnDesktop: Boolean = true,
        onClick: () -> Unit,
    ) {
        _items.add(
            AltMenuItem(
                text,
                leadingIcon,
                trailingIcon,
                enabled,
                colors,
                contentPadding,
                interactionSource,
                showOnDesktop,
                onClick,
            )
        )
    }
}
