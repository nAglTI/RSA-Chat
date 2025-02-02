package com.hypergonial.chat.view.composables

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A text field for entering a password.
 *
 * @param value The current value of the text field.
 * @param onValueChange The callback to be called when the value of the text field changes.
 * @param errorComposable The composable to be displayed when there is an error.
 * @param modifier The modifier to be applied to the text field.
 * @param label The label to be displayed in the text field.
 * @param placeholder The placeholder to be displayed in the text field.
 * @param keyboardOptions The options to be applied to the keyboard.
 * @param keyboardActions The actions to be applied to the keyboard.
 */
@Composable
fun PasswordTextField(
    value: String,
    isError: Boolean = false,
    enabled: Boolean = true,
    errorComposable: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions =
        KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var showPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        placeholder = placeholder,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
        trailingIcon = {
            IconButton(
                onClick = { showPassword = !showPassword },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            ) {
                if (showPassword) {
                    VisibilityIcon()
                } else {
                    VisibilityOffIcon()
                }
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )

    if (isError) {
        errorComposable?.invoke()
    }
}
