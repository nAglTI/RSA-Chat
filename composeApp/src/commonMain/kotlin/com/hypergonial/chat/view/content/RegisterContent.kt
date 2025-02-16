package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.ChatTheme
import com.hypergonial.chat.view.components.RegisterComponent
import com.hypergonial.chat.view.composables.ChatButton
import com.hypergonial.chat.view.composables.FullScreenProgressIndicator
import com.hypergonial.chat.view.composables.PasswordTextField

@Composable
fun RegisterTopBar(component: RegisterComponent) {
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
fun RegisterContent(component: RegisterComponent) {
    val state by component.data.subscribeAsState()
    val focusManager = LocalFocusManager.current
    val snackBarState = remember { SnackbarHostState() }

    FullScreenProgressIndicator(state.isRegistering, "Creating account...") {
        Scaffold(topBar = { RegisterTopBar(component) }, snackbarHost = { SnackbarHost(snackBarState) }) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Register an account",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 60.dp),
                )

                OutlinedTextField(
                    value = state.username,
                    modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                    singleLine = true,
                    isError = state.usernameErrors.isNotEmpty(),
                    onValueChange = { component.onUsernameChange(it) },
                    label = { Text("Username*") },
                    leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Username") },
                    trailingIcon = {
                        if (state.checkingUsernameAvailability) CircularProgressIndicator(Modifier.size(25.dp))
                        else if (state.usernameErrors.isEmpty() && state.username.isNotBlank())
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Username available",
                                tint = ChatTheme.colorScheme.success,
                            )
                        else if (state.username.isNotEmpty())
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Username error",
                                tint = MaterialTheme.colorScheme.error,
                            )
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )

                for (error in state.usernameErrors) {
                    Row { Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                }

                PasswordTextField(
                    value = state.password.expose(),
                    label = { Text("Password*") },
                    onValueChange = { component.onPasswordChange(password = it) },
                    isError = state.passwordErrors.isNotEmpty(),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                )

                for (error in state.passwordErrors) {
                    Row { Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                }

                PasswordTextField(
                    value = state.passwordConfirm.expose(),
                    label = { Text("Confirm Password*") },
                    isError = state.passwordErrors.isNotEmpty(),
                    onValueChange = { component.onPasswordConfirmChange(it) },
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Go,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onGo = {
                                if (state.canRegister) {
                                    focusManager.clearFocus()
                                    component.onRegisterAttempt()
                                }
                            }
                        ),
                    modifier = Modifier.width(300.dp).padding(0.dp, 5.dp),
                )

                ChatButton(
                    modifier = Modifier.padding(0.dp, 15.dp, 0.dp, 0.dp).width(125.dp).height(45.dp),
                    onClick = {
                        focusManager.clearFocus()
                        component.onRegisterAttempt()
                    },
                    enabled = state.canRegister,
                ) {
                    Text("Register")
                }
            }
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        if (state.snackbarMessage.value.isEmpty()) {
            return@LaunchedEffect
        }

        snackBarState.showSnackbar(
            state.snackbarMessage.value,
            duration = SnackbarDuration.Long,
            withDismissAction = true,
        )
    }
}
