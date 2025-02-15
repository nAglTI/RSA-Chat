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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.components.UserSettingsComponent
import com.hypergonial.chat.view.composables.ChatButton
import com.hypergonial.chat.view.composables.UserAvatar

@Composable
fun UserSettingsTopBar(component: UserSettingsComponent) {
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
fun UserSettingsContent(component: UserSettingsComponent) {
    val state by component.data.subscribeAsState()
    val renderedName = state.displayName.ifEmpty { state.username }
    val interactionSource = remember { MutableInteractionSource() }
    val snackbarState = remember { SnackbarHostState() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Scaffold(topBar = { UserSettingsTopBar(component) }, snackbarHost = { SnackbarHost(snackbarState) }) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                IconButton(
                    onClick = component::onAvatarChangeRequested,
                    modifier =
                        Modifier.hoverable(interactionSource).pointerHoverIcon(PointerIcon.Hand).requiredSize(100.dp),
                ) {
                    UserAvatar(state.avatarUrl, renderedName, size = 100.dp)
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
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Change Avatar")
                        Text("Change Avatar", fontSize = 10.sp, softWrap = false)
                    }
                }
            }

            Column {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = component::onUsernameChange,
                    isError = state.usernameErrors.isNotEmpty(),
                    label = { Text("Username") },
                    placeholder = { Text("Enter a new username...") },
                    modifier = Modifier.padding(5.dp),
                    singleLine = true,
                )

                for (error in state.usernameErrors) {
                    Row {
                        Text(
                            error,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.widthIn(max = 280.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = component::onDisplayNameChange,
                    isError = state.displayNameErrors.isNotEmpty(),
                    label = { Text("Display Name") },
                    placeholder = { Text(state.username) },
                    modifier = Modifier.padding(5.dp),
                    singleLine = true,
                )

                for (error in state.displayNameErrors) {
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
