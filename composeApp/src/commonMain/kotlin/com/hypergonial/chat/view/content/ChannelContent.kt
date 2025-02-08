package com.hypergonial.chat.view.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.getIcon
import com.hypergonial.chat.model.Mime
import com.hypergonial.chat.trimFilename
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.composables.FileDropTarget
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.MessageList
import io.github.vinceglb.filekit.core.PlatformFile

@Composable
fun ChannelContent(component: ChannelComponent) {
    FileDropTarget(component::onFilesDropped) {
        val state by component.data.subscribeAsState()

        val canSend by
            remember(state.chatBarValue, state.pendingAttachments) {
                derivedStateOf { state.chatBarValue.text.isNotEmpty() || state.pendingAttachments.isNotEmpty() }
            }

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            // Is a LazyColumn wrapped in a custom composable
            MessageList(
                state.messageEntries,
                Modifier.fillMaxWidth().weight(1f),
                listState = state.listState,
                isCruising = state.isCruising,
                onMessagesLimitReach = component::onMoreMessagesRequested,
            )

            Column(
                Modifier.padding(20.dp, 0.dp, 20.dp, 20.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp),
                    )
            ) {
                PendingFilesList(state.pendingAttachments, onPendingFileCancel = component::onPendingFileCancel)

                ChatBar(
                    state.chatBarValue,
                    Modifier.fillMaxWidth(),
                    onValueChange = component::onChatBarContentChanged,
                    onEditLastRequested = component::onEditLastMessage,
                    leadingIcon = { FileUploadIcon(component) },
                    onLeadingIconClick = component::onFileUploadDropdownOpen,
                    trailingButtonEnabled = canSend,
                    onSubmit = component::onMessageSend,
                )
            }
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
fun PendingFilesList(
    pendingAttachments: List<PlatformFile>,
    modifier: Modifier = Modifier,
    onPendingFileCancel: (PlatformFile) -> Unit,
) {
    val shape = remember { RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp) }
    val mimes by remember(pendingAttachments) { derivedStateOf { pendingAttachments.map { Mime.fromUrl(it.name) } } }

    AnimatedVisibility(
        pendingAttachments.isNotEmpty(),
        modifier
            .fillMaxWidth()
            .clip(shape)
            .padding(start = 20.dp, end = 20.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, shape = shape),
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        // Min height to prevent the LazyRow from shrinking to 0 height before the anim finishes
        LazyRow(Modifier.defaultMinSize(0.dp, 25.dp)) {
            itemsIndexed(pendingAttachments) { i, attachment ->
                InputChip(
                    onClick = { onPendingFileCancel(attachment) },
                    label = { Text(attachment.name.trimFilename()) },
                    selected = true,
                    avatar = {
                        Icon(mimes[i]?.getIcon() ?: Icons.Outlined.FilePresent, contentDescription = "Attachment")
                    },
                    trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Cancel") },
                )
            }
        }
    }
}
