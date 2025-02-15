package com.hypergonial.chat.view.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hypergonial.chat.altClickable
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.rememberHorizontalTooltipPositionProvider

/**
 * A composable that represents a guild item in the sidebar. This can be an actual guild or any other item in that
 * sidebar area.
 *
 * @param tooltipText The text to be displayed in the tooltip.
 * @param icon The icon to be displayed in the item. The modifier passed to the composable should be applied to the
 *   icon.
 * @param isSelected Whether the item is selected or not.
 * @param isSystemItem Whether the item is a system item or not.
 * @param onEdit The callback to be called when the guild is edited.
 * @param onDelete The callback to be called when the guild is deleted.
 * @param onLeave The callback to be called when the guild is left.
 * @param onSelect The callback to be called when the item is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarGuildItem(
    tooltipText: String,
    guildId: Snowflake,
    icon: @Composable (Modifier) -> Unit,
    isSelected: Boolean,
    onInviteCodeCopy: (() -> Unit)? = null,
    isSystemItem: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onLeave: (() -> Unit)? = null,
    onSelect: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    DesktopOnlyTooltipBox(
        positionProvider = rememberHorizontalTooltipPositionProvider(3.dp),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState(isPersistent = true),
    ) {
        val cornerRadius by
            animateDpAsState(
                if (isSelected) 14.dp else 28.dp,
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            )
        var isAltMenuOpen by remember { mutableStateOf(false) }

        AltActionMenu(
            isAltMenuOpen,
            onDismissRequest = { isAltMenuOpen = false },
            altActions = {
                if (onInviteCodeCopy != null) {
                    item(
                        "Copy invite code",
                        leadingIcon = { Icon(Icons.Outlined.PersonAdd, contentDescription = "Copy join code") },
                    ) {
                        onInviteCodeCopy()
                    }
                }

                if (onEdit != null) {
                    item(
                        "TODO - Edit Guild",
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "Edit Guild") },
                    ) {
                        onEdit()
                    }
                }

                if (onLeave != null) {
                    item(
                        "Leave Guild",
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Leave Guild") },
                    ) {
                        onLeave()
                    }
                }

                if (onDelete != null) {
                    item(
                        "Delete Guild",
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete Guild") },
                    ) {
                        onDelete()
                    }
                }

                if (settings.getDevSettings().isInDeveloperMode) {
                    item(
                        "Copy Guild ID",
                        leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = "Developer Mode") },
                    ) {
                        clipboardManager.setText(AnnotatedString(guildId.toString()))
                    }
                }
            },
        ) {
            IconButton(
                onClick = onSelect,
                Modifier.pointerHoverIcon(PointerIcon.Hand)
                    .padding(all = 2.dp)
                    .height(56.dp)
                    .width(56.dp)
                    .clip(RoundedCornerShape(cornerRadius))
                    .altClickable { isAltMenuOpen = !isAltMenuOpen }
                    .background(if (isSystemItem) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
            ) {
                val imageModifier =
                    Modifier.clip(RoundedCornerShape(cornerRadius))
                        .fillMaxSize()
                        .padding(vertical = 6.dp, horizontal = 6.dp)
                        .border(
                            1.5f.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(cornerRadius),
                        )

                icon(imageModifier)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarGuildItem(
    tooltipText: String,
    icon: @Composable (Modifier) -> Unit,
    isSelected: Boolean,
    isSystemItem: Boolean = false,
    onSelect: () -> Unit,
) {
    DesktopOnlyTooltipBox(
        positionProvider = rememberHorizontalTooltipPositionProvider(3.dp),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState(isPersistent = true),
    ) {
        val cornerRadius by
            animateDpAsState(
                if (isSelected) 14.dp else 28.dp,
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            )

        IconButton(
            onClick = onSelect,
            Modifier.pointerHoverIcon(PointerIcon.Hand)
                .padding(all = 2.dp)
                .height(56.dp)
                .width(56.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(if (isSystemItem) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
        ) {
            val imageModifier =
                Modifier.clip(RoundedCornerShape(cornerRadius))
                    .fillMaxSize()
                    .padding(vertical = 6.dp, horizontal = 6.dp)
                    .border(
                        1.5f.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(cornerRadius),
                    )

            icon(imageModifier)
        }
    }
}

/**
 * A composable that represents a guild icon. It displays the guild's icon if it exists, otherwise it displays a default
 * icon.
 *
 * @param guild The guild to be displayed.
 * @param modifier The modifier to be applied to the icon.
 */
@Composable
fun GuildIcon(guild: Guild, modifier: Modifier = Modifier) {
    if (guild.avatarUrl == null) {
        Icon(Icons.Outlined.Group, contentDescription = guild.name, modifier = modifier)
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current).data(guild.avatarUrl).crossfade(true).build(),
            contentDescription = guild.name,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}
