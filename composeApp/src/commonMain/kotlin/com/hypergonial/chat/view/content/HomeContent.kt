package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.HomeComponent
import com.hypergonial.chat.view.composables.MessageList

@Composable
fun HomeContent(component: HomeComponent) {
    val state by component.data.subscribeAsState()

    Scaffold { padding ->
        MessageList(
            features = state.messages,
            modifier = Modifier.fillMaxWidth().padding(padding),
            onMessagesLimitReach = component::onMoreMessagesRequested
        )
        Button(onClick = component::onLogoutClicked) {
            Text("Logout")
        }
    }
}
