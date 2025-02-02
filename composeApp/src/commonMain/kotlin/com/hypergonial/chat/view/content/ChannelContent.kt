package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.MessageList

@Composable
fun ChannelContent(component: ChannelComponent) {
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

        ChatBar(
            value = state.chatBarValue,
            onValueChange = component::onChatBarContentChanged,
            onEditLastRequested = component::onEditLastMessage,
            leadingIcon = {
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
                            leadingIcon = {
                                Icon(Icons.Outlined.Attachment, contentDescription = "Upload File")
                            },
                        )

                        DropdownMenuItem(
                            text = { Text("Upload Media") },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            onClick = { component.onFileAttachRequested(isMedia = true) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Image, contentDescription = "Upload Media")
                            },
                        )
                    }
                }
            },
            onLeadingIconClick = component::onFileUploadDropdownOpen,
            onSubmit = component::onMessageSend,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
