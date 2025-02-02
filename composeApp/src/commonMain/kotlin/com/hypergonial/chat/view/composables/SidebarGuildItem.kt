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
import androidx.compose.material.icons.outlined.Group
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hypergonial.chat.model.payloads.Guild
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
 * @param onSelect The callback to be called when the item is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarGuildItem(
    tooltipText: String,
    icon: @Composable (Modifier) -> Unit,
    isSelected: Boolean,
    isSystemItem: Boolean = false,
    onSelect: () -> Unit,
) {
    TooltipBox(
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
                        2.dp,
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
