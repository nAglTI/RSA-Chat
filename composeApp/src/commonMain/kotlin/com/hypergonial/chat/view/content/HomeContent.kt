package com.hypergonial.chat.view.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowWidthSizeClass
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.HomeComponent
import com.hypergonial.chat.view.composables.AdaptiveDrawer
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.ChatButton
import com.hypergonial.chat.view.composables.MessageList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onNavBarOpen: () -> Unit) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) {
        windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
    }

    TopAppBar(title = { Text("Chat") }, navigationIcon = {
        AnimatedVisibility(visible = isSmall) {
            IconButton(onClick = onNavBarOpen) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    })
}

@Composable
fun HomeContent(component: HomeComponent) {
    val state by component.data.subscribeAsState()
    val scope = rememberCoroutineScope()

    AdaptiveDrawer(drawerState = state.navDrawerState, drawerContent = {
        Text("Left Drawer content")
    }) {
        Scaffold(topBar = { HomeTopBar { scope.launch { state.navDrawerState.apply { if (isClosed) open() else close() } } } }) {
            Column(
                Modifier.safeDrawingPadding().fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
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
    }
}
