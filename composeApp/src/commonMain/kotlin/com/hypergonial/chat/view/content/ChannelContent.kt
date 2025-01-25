package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.ChatButton
import com.hypergonial.chat.view.composables.MessageList

@Composable
fun ChannelContent(component: ChannelComponent) {
    val state by component.data.subscribeAsState()

    Column(
        Modifier.safeDrawingPadding().fillMaxSize(), verticalArrangement = Arrangement.Bottom
    ) {
        ChatButton(onClick = component::onLogoutClicked) {
            Text("Logout")
        }

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
            onSubmit = component::onMessageSend,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

