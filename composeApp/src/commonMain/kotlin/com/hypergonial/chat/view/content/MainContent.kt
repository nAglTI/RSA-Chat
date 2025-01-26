package com.hypergonial.chat.view.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.window.core.layout.WindowWidthSizeClass
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.components.FallbackMainComponent
import com.hypergonial.chat.view.components.HomeComponent
import com.hypergonial.chat.view.components.SidebarComponent
import com.hypergonial.chat.view.composables.AdaptiveDrawer
import com.hypergonial.chat.view.composables.FullScreenSpinner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(text: String, onNavBarOpen: () -> Unit) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) {
        windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
    }

    TopAppBar(title = { Text(text) }, navigationIcon = {
        AnimatedVisibility(visible = isSmall) {
            IconButton(onClick = onNavBarOpen) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    })
}

@Composable
fun MainContent(component: SidebarComponent) {
    val state by component.data.subscribeAsState()
    val mainContent by component.mainContent.subscribeAsState()
    val scope = rememberCoroutineScope()


    FullScreenSpinner(state.isConnecting, "Connecting...") {
        AdaptiveDrawer(drawerState = state.navDrawerState, drawerContent = {
            Column {
                Text("Left Drawer content")
                Button(onClick = { component.onLogoutClicked() }) {
                    Text("Logout")
                }
            }

        }) {
            Scaffold(topBar = {
                MainTopBar("Chat") { scope.launch { state.navDrawerState.apply { if (isClosed) open() else close() } } }
            }) {
                when(val c = mainContent.child?.instance) {
                    is HomeComponent -> HomeContent(c)
                    is ChannelComponent -> ChannelContent(c)
                    is FallbackMainComponent -> FallbackContent(c)
                    else -> error("Unknown child: $c")
                }
            }
        }
    }

}
