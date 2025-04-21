package com.hypergonial.chat.view.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.hypergonial.chat.altClickable
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.settings

/**
 * A composable that represents a channel item in the sidebar.
 *
 * @param label The label to display for the channel.
 * @param isSelected Whether the channel is selected.
 * @param icon The icon to display for the channel.
 * @param onSelect The callback to call when the channel is selected.
 */
@Suppress("UnusedParameter")
@Composable
fun SidebarChannelItem(
    label: String,
    channelId: Snowflake,
    isSelected: Boolean,
    icon: @Composable () -> Unit = { Icon(Icons.Filled.Tag, contentDescription = "Channel Icon") },
    isUnread: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onSelect: () -> Unit,
) {
    var isAltMenuActive by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    AltActionMenu(
        isAltMenuActive,
        onDismissRequest = { isAltMenuActive = false },
        altActions = {
            /*
            if (onEdit != null) {

                item("TODO - Edit", leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "Edit Icon") }) {
                    onEdit()
                }

            }
            */

            if (onDelete != null) {
                item("Delete", leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete Icon") }) {
                    onDelete()
                }
            }

            if (settings.getDevSettings().isInDeveloperMode) {
                item(
                    "Copy Channel ID",
                    leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = "Developer Mode") },
                ) {
                    clipboardManager.setText(AnnotatedString(channelId.toString()))
                }
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth()
                .padding(1.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .clickable(onClick = onSelect)
                .altClickable { isAltMenuActive = !isAltMenuActive }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(5.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides
                    if (isSelected || isUnread) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f).compositeOver(Color.Gray)
            ) {
                Box(modifier = Modifier.padding(end = 5.dp)) { icon() }
                Text(label, softWrap = false)
            }
        }
    }
}

/**
 * A composable that represents a channel item in the sidebar.
 *
 * @param label The label to display for the channel.
 * @param isSelected Whether the channel is selected.
 * @param icon The icon to display for the channel.
 * @param onSelect The callback to call when the channel is selected.
 */
@Composable
fun SidebarChannelItem(
    label: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit = { Icon(Icons.Filled.Tag, contentDescription = "Channel Icon") },
    onSelect: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onSelect)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(5.dp)
    ) {
        Box(modifier = Modifier.padding(end = 5.dp)) { icon() }
        Text(label, softWrap = false)
    }
}
