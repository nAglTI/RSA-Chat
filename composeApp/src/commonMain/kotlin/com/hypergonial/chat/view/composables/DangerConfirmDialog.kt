package com.hypergonial.chat.view.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

/**
 * A dialog that prompts the user to confirm a dangerous action.
 *
 * @param key The key to identify the dialog. If the key is null, the dialog will not be shown.
 * @param prompt The prompt to display to the user.
 * @param confirm The text to display on the confirm button.
 * @param cancel The text to display on the cancel button.
 * @param onConfirm The callback to invoke when the user confirms the action. The key is passed as a parameter.
 * @param onCancel The callback to invoke when the user cancels the action. The key is passed as a parameter.
 */
@Composable
fun <T> DangerConfirmDialog(
    key: T? = null,
    title: AnnotatedString,
    prompt: AnnotatedString,
    confirm: AnnotatedString,
    cancel: AnnotatedString,
    onConfirm: (T) -> Unit,
    onCancel: (T) -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    EditorFocusInhibitor("DANGER_CONFIRM_DIALOG", key != null)

    if (key == null) {
        return
    }

    var isConfirmed by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Clear focus when the overlay activates (to prevent the IME staying open)
    LaunchedEffect(key) { focusManager.clearFocus() }

    AnimatedDialog(
        onDismissRequest = { if (isConfirmed) onConfirm(key) else onCancel(key) },
        enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut() + scaleOut(spring(stiffness = Spring.StiffnessMedium)),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier.width(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(vertical = 15.dp, horizontal = 15.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)

            Text(prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))

            if (content != null) {
                Box(
                    Modifier.heightIn(0.dp, 400.dp)
                        .padding(top = 15.dp)
                        .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = {
                        isConfirmed = false
                        dismissWithAnimation()
                    },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(cancel)
                }

                Button(
                    onClick = {
                        isConfirmed = true
                        dismissWithAnimation()
                    },
                    modifier = Modifier.padding(start = 10.dp).pointerHoverIcon(PointerIcon.Hand),
                    colors =
                        ButtonDefaults.buttonColors()
                            .copy(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                ) {
                    Text(confirm)
                }
            }
        }
    }
}

/**
 * A dialog that prompts the user to confirm a dangerous action with a challenge string.
 *
 * @param key The key to identify the dialog. If the key is null, the dialog will not be shown.
 * @param prompt The prompt to display to the user.
 * @param confirm The text to display on the confirm button.
 * @param cancel The text to display on the cancel button.
 * @param challengeTarget The string the user has to type to confirm the action.
 * @param onConfirm The callback to invoke when the user confirms the action. The key is passed as a parameter.
 * @param onCancel The callback to invoke when the user cancels the action. The key is passed as a parameter.
 */
@Composable
fun <T> SuperDangerConfirmDialog(
    key: T? = null,
    title: AnnotatedString,
    prompt: AnnotatedString,
    confirm: AnnotatedString,
    cancel: AnnotatedString,
    challengeTarget: String,
    challengeLabel: String,
    onConfirm: (T) -> Unit,
    onCancel: (T) -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    EditorFocusInhibitor("SUPER_DANGER_CONFIRM_DIALOG", key != null)

    if (key == null) {
        return
    }

    var challengeValue by remember { mutableStateOf("") }

    var isConfirmed by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Clear focus when the overlay activates (to prevent the IME staying open)
    LaunchedEffect(key) { focusManager.clearFocus() }

    AnimatedDialog(
        onDismissRequest = { if (isConfirmed) onConfirm(key) else onCancel(key) },
        enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut() + scaleOut(spring(stiffness = Spring.StiffnessMedium)),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier.width(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(vertical = 15.dp, horizontal = 15.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)

            Text(prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))

            if (content != null) {
                Box(
                    Modifier.heightIn(0.dp, 400.dp)
                        .padding(top = 15.dp)
                        .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
            }

            OutlinedTextField(
                challengeValue,
                onValueChange = { challengeValue = it.take(100) },
                label = { Text(challengeLabel) },
                placeholder = { Text(challengeTarget) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
            )

            Row(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = {
                        isConfirmed = false
                        dismissWithAnimation()
                    },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(cancel)
                }

                Button(
                    enabled = challengeValue == challengeTarget,
                    onClick = {
                        isConfirmed = true
                        dismissWithAnimation()
                    },
                    modifier = Modifier.padding(start = 10.dp).pointerHoverIcon(PointerIcon.Hand),
                    colors =
                        ButtonDefaults.buttonColors()
                            .copy(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                ) {
                    Text(confirm)
                }
            }
        }
    }
}
