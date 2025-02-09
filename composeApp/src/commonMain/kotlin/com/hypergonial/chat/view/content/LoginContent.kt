package com.hypergonial.chat.view.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import chat.composeapp.generated.resources.Res
import chat.composeapp.generated.resources.compose_multiplatform
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.LoginComponent
import com.hypergonial.chat.view.composables.ActionText
import com.hypergonial.chat.view.composables.ChatButton
import com.hypergonial.chat.view.composables.FullScreenProgressIndicator
import com.hypergonial.chat.view.composables.PasswordTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoginBottomBar(component: LoginComponent) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(0.dp, 0.dp, 0.dp, 30.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ActionText(
            "Don't have an account? Sign up",
            actionLinks = mapOf("Sign up" to { component.onRegisterRequested() }),
            fontSize = 16.sp,
        )
    }
}

@Composable
fun LoginContent(component: LoginComponent) {
    val state by component.data.subscribeAsState()
    val focusManager = LocalFocusManager.current
    val snackbarState = remember { SnackbarHostState() }

    FullScreenProgressIndicator(state.isLoggingIn, "Logging in...") {
        Scaffold(bottomBar = { LoginBottomBar(component) }, snackbarHost = { SnackbarHost(snackbarState) }) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(0.dp, 0.dp, 0.dp, 50.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.compose_multiplatform),
                    contentDescription = "Logo",
                    modifier =
                        Modifier.size(200.dp).padding(0.dp, 0.dp, 0.dp, 50.dp).clickable(
                            interactionSource = null,
                            indication = null,
                        ) {
                            component.onLogoClick()
                        },
                )

                OutlinedTextField(
                    value = state.username,
                    isError = state.loginFailed,
                    modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                    singleLine = true,
                    enabled = !state.isLoggingIn,
                    onValueChange = { component.onUsernameChange(username = it) },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Username") },
                    placeholder = { Text("Enter your username...") },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )

                PasswordTextField(
                    value = state.password.expose(),
                    isError = state.loginFailed,
                    enabled = !state.isLoggingIn,
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password...") },
                    onValueChange = { component.onPasswordChange(password = it) },
                    keyboardActions = KeyboardActions(onDone = { if (state.canLogin) component.onLoginAttempt() }),
                    modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                )

                ChatButton(
                    modifier = Modifier.padding(0.dp, 15.dp, 0.dp, 0.dp).width(125.dp).height(45.dp),
                    onClick = component::onLoginAttempt,
                    enabled = state.canLogin && !state.isLoggingIn,
                ) {
                    Text("Login")
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
