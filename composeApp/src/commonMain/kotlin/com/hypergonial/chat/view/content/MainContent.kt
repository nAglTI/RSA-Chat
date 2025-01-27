package com.hypergonial.chat.view.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PlusOne
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
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
            Row {
                LazyColumn {
                    itemsIndexed(state.guilds, key = { _, item -> item.id }) { _, guild ->
                        IconButton(onClick = { component.onGuildSelected(guild.id) }) {
                            if (guild.avatarUrl != null) {
                                Icon(Icons.Outlined.Group, contentDescription = guild.name)
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                        .data(guild.avatarUrl).crossfade(true).build(),
                                    contentDescription = guild.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 14.dp)
                                        .clip(
                                            CircleShape
                                        ).height(40.dp).width(40.dp),
                                )
                            }
                        }
                    }

                    item {
                        IconButton(onClick = { component.onGuildCreateClicked() }) {
                            Icon(Icons.Filled.Add, contentDescription = "New Guild")
                        }
                    }
                }
                LazyColumn {
                    itemsIndexed(state.channels, key = { _, item -> item.id }) { _, channel ->
                        Row(Modifier.clickable(onClick = { component.onChannelSelected(channel.id) })) {
                            Icon(Icons.Filled.Tag, contentDescription = "Channel Icon")
                            Text(channel.name)
                        }
                    }

                    if (state.selectedGuild != null) {
                        item {
                            Row(Modifier.clickable(onClick = { component.onChannelCreateClicked() })) {
                                Icon(Icons.Filled.Add, contentDescription = "New Channel")
                                Text("New Channel")
                            }
                        }
                    }


                    item {
                        Button(onClick = { component.onLogoutClicked() }) {
                            Text("Logout")
                        }
                    }
                }
            }

        }) {
            Scaffold(topBar = {
                MainTopBar("Chat") { scope.launch { state.navDrawerState.apply { if (isClosed) open() else close() } } }
            }) {
                when (val c = mainContent.child?.instance) {
                    is HomeComponent -> HomeContent(c)
                    is ChannelComponent -> ChannelContent(c)
                    is FallbackMainComponent -> FallbackContent(c)
                    else -> error("Unknown child: $c")
                }
            }
        }
    }
}
