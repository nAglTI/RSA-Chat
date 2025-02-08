package com.hypergonial.chat.view.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.toggle
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.components.FallbackMainComponent
import com.hypergonial.chat.view.components.HomeComponent
import com.hypergonial.chat.view.components.SidebarComponent
import com.hypergonial.chat.view.composables.AdaptiveDrawer
import com.hypergonial.chat.view.composables.AssetViewerOverlay
import com.hypergonial.chat.view.composables.FullScreenProgressIndicator
import com.hypergonial.chat.view.composables.GuildIcon
import com.hypergonial.chat.view.composables.SidebarChannelItem
import com.hypergonial.chat.view.composables.SidebarGuildItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(component: SidebarComponent) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }
    val scope = rememberCoroutineScope()
    val state by component.data.subscribeAsState()

    val topBarText by
        remember(state.selectedChannel) {
            derivedStateOf { state.selectedChannel?.name ?: state.selectedGuild?.name ?: "Home" }
        }

    val icon by
        remember(state.selectedChannel) {
            derivedStateOf { if (state.selectedChannel != null) Icons.Filled.Tag else Icons.Filled.Home }
        }

    TopAppBar(
        title = {
            Row {
                Icon(icon, contentDescription = "Channel Icon", Modifier.padding(end = 5.dp))
                Text(topBarText)
            }
        },
        navigationIcon = {
            AnimatedVisibility(visible = isSmall) {
                IconButton(onClick = { scope.launch { state.navDrawerState.toggle() } }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            }
        },
    )
}

@Composable
fun SidebarContent(
    component: SidebarComponent
) {
    val state by component.data.subscribeAsState()

    Row {
        LazyColumn(
            Modifier.fillMaxHeight()
                .background(
                    if (state.navDrawerState.targetValue == DrawerValue.Open) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
                .safeDrawingPadding()
        ) {
            item {
                SidebarGuildItem(
                    tooltipText = "Home",
                    icon = { modifier ->
                        Icon(Icons.Outlined.Home, contentDescription = "Home", modifier = modifier)
                    },
                    isSelected = state.selectedGuild == null,
                    onSelect = { component.onHomeSelected() },
                )
            }

            itemsIndexed(state.guilds, key = { _, item -> item.id.toString() }) { _, guild ->
                SidebarGuildItem(
                    tooltipText = guild.name,
                    icon = { modifier -> GuildIcon(guild, modifier) },
                    isSelected = guild.id == state.selectedGuild?.id,
                    onSelect = { component.onGuildSelected(guild.id) },
                )
            }

            item {
                SidebarGuildItem(
                    tooltipText = "New Guild",
                    icon = { modifier ->
                        Icon(Icons.Outlined.Add, contentDescription = "New Guild", modifier = modifier)
                    },
                    isSelected = false,
                    onSelect = { component.onGuildCreateClicked() },
                )
            }
        }
        Column(Modifier.safeDrawingPadding().padding(start = 5.dp)) {
            Text(state.selectedGuild?.name ?: "Home", Modifier.padding(vertical = 5.dp))

            HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 5.dp))

            LazyColumn {
                itemsIndexed(state.channels, key = { _, item -> item.id.toString() }) { _, channel ->
                    SidebarChannelItem(
                        label = channel.name,
                        isSelected = channel.id == state.selectedChannel?.id,
                        onSelect = { component.onChannelSelected(channel.id) },
                    )
                }

                if (state.selectedGuild != null) {
                    item {
                        SidebarChannelItem(
                            label = "New Channel",
                            isSelected = false,
                            icon = { Icon(Icons.Filled.Add, contentDescription = "New Channel") },
                            onSelect = { component.onChannelCreateClicked() },
                        )
                    }
                }

                if (state.selectedGuild == null) {
                    item {
                        SidebarChannelItem(
                            label = "Logout",
                            isSelected = false,
                            icon = {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                            },
                            onSelect = { component.onLogoutClicked() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(component: SidebarComponent) {
    val state by component.data.subscribeAsState()
    val mainContent by component.mainContent.subscribeAsState()

    FullScreenProgressIndicator(state.isConnecting, "Connecting...") {
        AssetViewerOverlay(state.assetViewerActive, state.assetViewerUrl, component::onAssetViewerClosed) {
            AdaptiveDrawer(
                drawerState = state.navDrawerState,
                drawerContent = { SidebarContent(component) },
            ) {
                Scaffold(topBar = { MainTopBar(component) }) { padding ->
                    Box(Modifier.padding(padding).imePadding()) {
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
    }
}
