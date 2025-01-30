package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.MessageList

@Composable
fun ChannelContent(component: ChannelComponent) {
    val state by component.data.subscribeAsState()

    Column(
        Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom
    ) {
        // Is a LazyColumn wrapped in a custom composable
        MessageList(
            features = state.messageEntries,
            modifier = Modifier.fillMaxWidth().weight(1f),
            listState = state.listState,
            isCruising = state.isCruising,
            onMessagesLimitReach = component::onMoreMessagesRequested
        )

        ChatBar(
            value = state.chatBarValue,
            onValueChange = component::onChatBarContentChanged,
            onEditLastRequested = component::onEditLastMessage,
            leadingIcon = {
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = "Upload File",
                    modifier = Modifier.pointerHoverIcon(
                        PointerIcon.Hand
                    )
                )
            },
            onLeadingIconClick = component::onFileUploadRequested,
            onSubmit = component::onMessageSend,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

