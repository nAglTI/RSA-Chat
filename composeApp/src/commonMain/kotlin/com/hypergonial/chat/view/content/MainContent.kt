package com.hypergonial.chat.view.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.altClickable
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.platform
import com.hypergonial.chat.toggle
import com.hypergonial.chat.view.ChatTheme
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.components.FallbackMainComponent
import com.hypergonial.chat.view.components.HomeComponent
import com.hypergonial.chat.view.components.MainComponent
import com.hypergonial.chat.view.composables.AdaptiveDrawer
import com.hypergonial.chat.view.composables.AltActionMenu
import com.hypergonial.chat.view.composables.AssetViewerDialog
import com.hypergonial.chat.view.composables.FullScreenProgressIndicator
import com.hypergonial.chat.view.composables.GuildIcon
import com.hypergonial.chat.view.composables.SidebarChannelItem
import com.hypergonial.chat.view.composables.SidebarGuildItem
import com.hypergonial.chat.view.composables.Avatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(component: MainComponent, drawerState: DrawerState) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }
    val scope = rememberCoroutineScope()
    val state by component.data.subscribeAsState()
    var altMenuState by remember { mutableStateOf(false) }
    val ownsCurrentGuild = remember(state.selectedGuild) { state.selectedGuild?.ownerId == state.currentUser?.id }
    val clipboardManager = LocalClipboardManager.current

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
            if (state.selectedChannel != null) {
                AltActionMenu(
                    altMenuState,
                    onDismissRequest = { altMenuState = false },
                    altActions = {
                        if (ownsCurrentGuild) {
                            item(
                                "TODO - Edit",
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "Edit Icon") },
                            ) {
                                component.onChannelEditClicked(state.selectedChannel!!.id)
                            }
                        }

                        if (ownsCurrentGuild) {
                            item(
                                "Delete",
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete Icon") },
                            ) {
                                component.onChannelDeleteClicked(state.selectedChannel!!.id)
                            }
                        }

                        if (settings.getDevSettings().isInDeveloperMode && state.selectedChannel != null) {
                            item(
                                "Copy Channel ID",
                                leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = "Developer Mode") },
                            ) {
                                clipboardManager.setText(AnnotatedString(state.selectedChannel?.id.toString()))
                            }
                        }
                    },
                ) {
                    Row(Modifier.altClickable { altMenuState = true }) {
                        Icon(icon, contentDescription = "Channel Icon", Modifier.padding(end = 5.dp))
                        Text(topBarText)
                    }
                }
            } else {
                Row {
                    Icon(icon, contentDescription = "Channel Icon", Modifier.padding(end = 5.dp))
                    Text(topBarText)
                }
            }
        },
        navigationIcon = {
            AnimatedVisibility(visible = isSmall) {
                // Flash menu icon when no guild is selected
                val borderColor =
                    if (state.selectedGuild == null && isSmall && platform.isDesktopOrWeb()) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val flashingAlpha by
                            infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec =
                                    infiniteRepeatable(
                                        animation = tween(durationMillis = 1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse,
                                    ),
                            )
                        MaterialTheme.colorScheme.primary.copy(alpha = flashingAlpha)
                    } else Color.Transparent

                IconButton(
                    onClick = { scope.launch { drawerState.toggle() } },
                    modifier = Modifier.border(1.5f.dp, borderColor, shape = CircleShape),
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            }
        },
    )
}

@Composable
fun SidebarContent(component: MainComponent, drawerState: DrawerState) {
    val state by component.data.subscribeAsState()
    val clipboardManager = LocalClipboardManager.current

    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxHeight()
                .background(
                    if (drawerState.targetValue == DrawerValue.Open) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
                .statusBarsPadding()
        ) {
            item {
                SidebarGuildItem(
                    tooltipText = "Home",
                    icon = { modifier ->
                        Crossfade(state.selectedGuild == null) { isSelected ->
                            if (isSelected) {
                                Icon(Icons.Filled.Home, contentDescription = "Home", modifier = modifier)
                            } else {
                                Icon(Icons.Outlined.Home, contentDescription = "Home", modifier = modifier)
                            }
                        }
                    },
                    isSelected = state.selectedGuild == null,
                    onSelect = { component.onHomeSelected() },
                )
            }

            itemsIndexed(state.guilds, key = { _, item -> item.id.toString() }) { _, guild ->
                if (state.currentUser?.id == guild.ownerId) {
                    SidebarGuildItem(
                        tooltipText = guild.name,
                        guildId = guild.id,
                        icon = { modifier -> GuildIcon(guild, guild.id == state.selectedGuild?.id, modifier) },
                        isSelected = guild.id == state.selectedGuild?.id,
                        isUnread = state.guildReadStates[guild.id] ?: false,
                        onSelect = { component.onGuildSelected(guild.id) },
                        onEdit = { component.onGuildEditClicked(guild.id) },
                        onDelete = { component.onGuildDeleteClicked(guild.id) },
                        onInviteCodeCopy = { clipboardManager.setText(AnnotatedString(guild.id.toString())) },
                    )
                } else {
                    SidebarGuildItem(
                        tooltipText = guild.name,
                        guildId = guild.id,
                        icon = { modifier -> GuildIcon(guild, guild.id == state.selectedGuild?.id, modifier) },
                        isUnread = state.guildReadStates[guild.id] ?: false,
                        isSelected = guild.id == state.selectedGuild?.id,
                        onSelect = { component.onGuildSelected(guild.id) },
                        onLeave = { component.onGuildLeaveClicked(guild.id) },
                    )
                }
            }

            item {
                SidebarGuildItem(
                    tooltipText = "New Guild",
                    icon = { modifier ->
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "New Guild",
                            modifier = modifier,
                            tint = ChatTheme.colorScheme.success,
                        )
                    },
                    isSelected = false,
                    onSelect = { component.onGuildCreateClicked() },
                )
            }
        }
        Column(Modifier.safeDrawingPadding().padding(start = 5.dp).fillMaxHeight()) {
            Text(
                state.selectedGuild?.name ?: "Home",
                Modifier.padding(vertical = 5.dp, horizontal = 10.dp),
                style = MaterialTheme.typography.headlineSmall,
            )

            HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 5.dp))

            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(state.channels, key = { _, item -> item.id.toString() }) { _, channel ->
                    if (state.currentUser?.id == state.selectedGuild?.ownerId) {
                        SidebarChannelItem(
                            label = channel.name,
                            channelId = channel.id,
                            isSelected = channel.id == state.selectedChannel?.id,
                            isUnread = state.channelReadStates[channel.id] ?: false,
                            onSelect = { component.onChannelSelected(channel.id) },
                            onEdit = { component.onChannelEditClicked(channel.id) },
                            onDelete = { component.onChannelDeleteClicked(channel.id) },
                        )
                    } else {
                        SidebarChannelItem(
                            label = channel.name,
                            channelId = channel.id,
                            isSelected = channel.id == state.selectedChannel?.id,
                            isUnread = state.channelReadStates[channel.id] ?: false,
                            onSelect = { component.onChannelSelected(channel.id) },
                        )
                    }
                }

                if (state.selectedGuild != null && state.currentUser?.id == state.selectedGuild?.ownerId) {
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
                            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout") },
                            onSelect = { component.onLogoutClicked() },
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(state.currentUser?.avatarUrl, state.currentUser?.resolvedName ?: "Unknown", size = 35.dp)
                    Text(state.currentUser?.resolvedName ?: "Connecting...", Modifier.padding(start = 5.dp))
                }
                IconButton(onClick = component::onUserSettingsClicked) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        Modifier.pointerHoverIcon(PointerIcon.Hand),
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(component: MainComponent) {
    val state by component.data.subscribeAsState()
    val mainContent by component.mainContent.subscribeAsState()
    val snackbarState = remember { SnackbarHostState() }
    val navDrawerState = remember { DrawerState(DrawerValue.Closed) }

    FullScreenProgressIndicator(state.isConnecting, state.connectingMessage)
    AssetViewerDialog(state.assetViewerActive, state.assetViewerUrl, component::onAssetViewerClosed)

    AdaptiveDrawer(
        drawerState = navDrawerState,
        drawerContent = { SidebarContent(component, navDrawerState) },
        // NOTE: This must be here otherwise the Android Studio layout inspector violently explodes
        // Relevant Bug Report: https://issuetracker.google.com/issues/258053978
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        Scaffold(topBar = { MainTopBar(component, navDrawerState) }, snackbarHost = { SnackbarHost(snackbarState) }) {
            padding ->
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

    LaunchedEffect(state.snackbarMessage) {
        if (state.snackbarMessage.value.isNotEmpty()) {
            snackbarState.showSnackbar(state.snackbarMessage.value, withDismissAction = true)
        }
    }

    LaunchedEffect(state.navDrawerCommand) {
        when (state.navDrawerCommand.value) {
            MainComponent.NavDrawerCommand.OPEN -> navDrawerState.open()
            MainComponent.NavDrawerCommand.CLOSE -> navDrawerState.close()
            MainComponent.NavDrawerCommand.CLOSE_WITHOUT_ANIMATION -> navDrawerState.snapTo(DrawerValue.Closed)
            MainComponent.NavDrawerCommand.OPEN_WITHOUT_ANIMATION -> navDrawerState.snapTo(DrawerValue.Open)
            MainComponent.NavDrawerCommand.TOGGLE -> navDrawerState.toggle()
        }
    }
}
