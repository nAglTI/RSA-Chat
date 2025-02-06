package com.hypergonial.chat.view.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.composables.AttachmentDropTarget
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.MessageList

@Composable
fun ChannelContent(component: ChannelComponent) {
    AttachmentDropTarget(component::onFilesDropped) {
        val state by component.data.subscribeAsState()

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            // Is a LazyColumn wrapped in a custom composable
            MessageList(
                features = state.messageEntries,
                modifier = Modifier.fillMaxWidth().weight(1f),
                listState = state.listState,
                isCruising = state.isCruising,
                onMessagesLimitReach = component::onMoreMessagesRequested,
            )

            PendingFilesList(component)

            ChatBar(
                value = state.chatBarValue,
                onValueChange = component::onChatBarContentChanged,
                onEditLastRequested = component::onEditLastMessage,
                leadingIcon = { FileUploadIcon(component) },
                onLeadingIconClick = component::onFileUploadDropdownOpen,
                onSubmit = component::onMessageSend,
                modifier = Modifier.fillMaxWidth().padding(20.dp, 0.dp, 20.dp, 20.dp),
            )
        }
    }
}

@Composable
fun FileUploadIcon(component: ChannelComponent) {
    val state by component.data.subscribeAsState()

    Box {
        Icon(
            Icons.Filled.AddCircle,
            contentDescription = "Upload Attachment",
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        )

        DropdownMenu(
            expanded = state.isFileUploadDropdownOpen,
            onDismissRequest = component::onFileUploadDropdownClose,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        ) {
            DropdownMenuItem(
                text = { Text("Upload File") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                onClick = { component.onFileAttachRequested(isMedia = false) },
                leadingIcon = { Icon(Icons.Outlined.Attachment, contentDescription = "Upload File") },
            )

            DropdownMenuItem(
                text = { Text("Upload Media") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                onClick = { component.onFileAttachRequested(isMedia = true) },
                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = "Upload Media") },
            )
        }
    }
}

@Composable
fun PendingFilesList(component: ChannelComponent, modifier: Modifier = Modifier) {
    val state by component.data.subscribeAsState()

    AnimatedVisibility(
        state.pendingAttachments.isNotEmpty(),
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        val shape = remember {
            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        }

        Box(
            modifier.fillMaxWidth()
                .clip(shape)
                .padding(start = 20.dp, end = 20.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, shape = shape)
        ) {
            LazyRow(Modifier.fillMaxWidth()) {
                items(state.pendingAttachments) {
                    InputChip(
                        onClick = { component.onPendingFileCancel(it) },
                        label = { Text(it.name.trimFilename()) },
                        selected = true,
                        avatar = { Icon(Icons.Outlined.Attachment, contentDescription = "Attachment") },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Cancel") },
                    )
                }
            }
        }
    }
}

fun String.trimFilename(): String {
    return if (length > 20) {
        val ext = substringAfterLast('.', missingDelimiterValue = "")

        substring(0, 15) + "{...}" + if (ext.isNotEmpty()) ".$ext" else ""
    } else {
        this
    }
}
