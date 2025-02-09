package com.hypergonial.chat.view.content.prompts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.components.prompts.CreateGuildComponent
import com.hypergonial.chat.view.composables.FullScreenProgressIndicator

@Composable
fun CreateGuildContent(component: CreateGuildComponent) {
    val state by component.data.subscribeAsState()
    val snackbarState = remember { SnackbarHostState() }

    FullScreenProgressIndicator(isActive = state.isLoading) {
        Scaffold(Modifier.fillMaxSize(), snackbarHost = { SnackbarHost((snackbarState)) }) {
            Box {
                if (platform.needsBackButton()) {
                    IconButton(
                        onClick = component::onBackClicked,
                        modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp).align(Alignment.TopStart),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    OutlinedTextField(
                        value = state.guildName,
                        onValueChange = component::onGuildNameChanged,
                        singleLine = true,
                        placeholder = { Text("Enter the name of your guild...") },
                        label = { Text("Guild Name") },
                    )

                    Button(
                        onClick = component::onGuildCreateClicked,
                        enabled = state.isCreateButtonEnabled,
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        if (state.snackbarMessage.value.isEmpty()) {
            return@LaunchedEffect
        }

        snackbarState.showSnackbar(
            state.snackbarMessage.value,
            duration = SnackbarDuration.Long,
            withDismissAction = true,
        )
    }
}
