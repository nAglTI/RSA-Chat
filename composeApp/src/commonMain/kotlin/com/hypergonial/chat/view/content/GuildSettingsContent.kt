package com.hypergonial.chat.view.content

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.altClickable
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.components.GuildSettingsComponent
import com.hypergonial.chat.view.composables.AltActionMenu
import com.hypergonial.chat.view.composables.ChatButton
import com.hypergonial.chat.view.composables.Avatar

@Composable
fun GuildSettingsTopBar(component: GuildSettingsComponent) {
    // Add a back button to the left of the top bar

    if (!platform.needsBackButton()) {
        return
    }

    Row(
        Modifier.fillMaxWidth().height(50.dp).padding(0.dp, 0.dp, 0.dp, 0.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = component::onBackClicked, modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }
}

@Composable
fun GuildSettingsContent(component: GuildSettingsComponent) {
    val state by component.data.subscribeAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val snackbarState = remember { SnackbarHostState() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isAltActionMenuOpen by remember { mutableStateOf(false) }

    Scaffold(topBar = { GuildSettingsTopBar(component) }, snackbarHost = { SnackbarHost(snackbarState) }) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                AltActionMenu(
                    isAltActionMenuOpen,
                    onDismissRequest = { isAltActionMenuOpen = false },
                    altActions = {
                        item(
                            "Change Avatar",
                            leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Change Avatar") },
                        ) {
                            component.onAvatarChangeRequested()
                        }

                        item(
                            "Remove Avatar",
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Remove Avatar") },
                        ) {
                            component.onAvatarRemoveRequested()
                        }
                    },
                ) {
                    IconButton(
                        onClick = component::onAvatarChangeRequested,
                        modifier =
                        Modifier.hoverable(interactionSource)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .altClickable { isAltActionMenuOpen = true }
                            .requiredSize(100.dp),
                    ) {
                        Avatar(state.avatarUrl, state.guildName, size = 100.dp)
                    }
                }

                // FIXME: Why is this not working without full path?
                androidx.compose.animation.AnimatedVisibility(isHovered, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        Modifier.height(100.dp)
                            .width(100.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Change Icon")
                        Text("Change Icon", fontSize = 10.sp, softWrap = false)
                    }
                }
            }

            Column {
                OutlinedTextField(
                    value = state.guildName,
                    onValueChange = component::onNameChange,
                    isError = state.guildNameErrors.isNotEmpty(),
                    label = { Text("Guild Name") },
                    placeholder = { Text("Enter a new name...") },
                    modifier = Modifier.padding(5.dp),
                    singleLine = true,
                )

                for (error in state.guildNameErrors) {
                    Row {
                        Text(
                            error,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.widthIn(max = 280.dp),
                        )
                    }
                }

                ChatButton(onClick = component::onSaveClicked, enabled = state.canSave) { Text("Save") }
            }
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        if (state.snackbarMessage.value.isNotBlank()) {
            snackbarState.showSnackbar(state.snackbarMessage.value)
        }
    }
}
