package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.components.DebugSettingsComponent
import com.hypergonial.chat.view.composables.ChatButton
import kotlinx.coroutines.launch

@Composable
fun DebugSettingsTopBar(component: DebugSettingsComponent) {
    // Add a back button to the left of the top bar

    if (!platform.needsBackButton()) {
        return
    }

    Row(
        Modifier.fillMaxWidth().height(50.dp).padding(0.dp, 0.dp, 0.dp, 0.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { component.onBackClicked() },
            modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }
}

@Composable
fun DebugSettingsContent(component: DebugSettingsComponent) {
    val state by component.data.subscribeAsState()
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = { DebugSettingsTopBar(component) },
        snackbarHost = { SnackbarHost(snackbarState) },
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight().safeDrawingPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Debug Settings",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 60.dp),
            )

            OutlinedTextField(
                value = state.apiEndpoint,
                modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                singleLine = true,
                isError = state.apiEndpointError,
                onValueChange = { component.onApiEndpointChange(it) },
                label = { Text("API Endpoint") },
                placeholder = { Text("https://example.org/api/v1") },
                leadingIcon = { Icon(Icons.Filled.Api, contentDescription = "API Endpoint") },
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            OutlinedTextField(
                value = state.gatewayEndpoint,
                modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                singleLine = true,
                isError = state.gatewayEndpointError,
                onValueChange = { component.onGatewayEndpointChange(it) },
                label = { Text("Gateway Endpoint") },
                placeholder = { Text("wss://example.org/gateway/v1") },
                leadingIcon = { Icon(Icons.Filled.Route, contentDescription = "Gateway Endpoint") },
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            OutlinedTextField(
                value = state.objectStoreEndpoint,
                modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                singleLine = true,
                isError = state.objectStoreEndpointError,
                onValueChange = { component.onObjectStoreEndpointChange(it) },
                label = { Text("Object Store endpoint") },
                placeholder = { Text("https://cdn.example.org") },
                leadingIcon = {
                    Icon(Icons.Filled.Description, contentDescription = "Object Store endpoint")
                },
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            if (
                                state.hasChanged &&
                                    !state.apiEndpointError &&
                                    !state.gatewayEndpointError &&
                                    !state.objectStoreEndpointError
                            ) {
                                component.onSaveClicked()
                                scope.launch { snackbarState.showSnackbar("Settings saved") }
                                focusManager.clearFocus()
                            }
                        }
                    ),
            )

            ChatButton(
                modifier = Modifier.padding(0.dp, 15.dp, 0.dp, 0.dp).width(125.dp).height(45.dp),
                onClick = {
                    component.onSaveClicked()
                    scope.launch { snackbarState.showSnackbar("Settings saved") }
                },
                enabled =
                    state.hasChanged &&
                        !state.apiEndpointError &&
                        !state.gatewayEndpointError &&
                        !state.objectStoreEndpointError,
            ) {
                Text("Save")
            }
        }
    }
}
