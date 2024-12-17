package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.HomeComponent
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.MessageList

@Composable
fun HomeContent(component: HomeComponent) {
    val state by component.data.subscribeAsState()

    Scaffold {
        Column(
            Modifier.safeDrawingPadding().fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Button(onClick = component::onLogoutClicked) {
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
                onTextChange = component::onChatBarContentChanged,
                onSend = component::onMessageSend,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
