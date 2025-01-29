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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

@Composable
fun SidebarChannelItem(
    label: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit = { Icon(Icons.Filled.Tag, contentDescription = "Channel Icon") },
    onSelect: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .clickable(onClick = onSelect)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(5.dp)
    ) {
        Box(modifier = Modifier.padding(end = 5.dp)) {
            icon()
        }
        Text(label, softWrap = false)
    }
}
