package com.hypergonial.chat.view.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

/**
 * A full-screen progress indicator that can be displayed over the content, blocking user interaction.
 *
 * @param isActive Whether the progress indicator should be displayed If false, the progress indicator will not be
 *   displayed and the content will be displayed as normal
 * @param loadingText The text to display below the progress indicator
 */
@Composable
fun FullScreenProgressIndicator(isActive: Boolean, loadingText: String? = null) {
    EditorFocusInhibitor("PROGRESS_INDICATOR", isActive)

    if (!isActive) {
        return
    }

    val focusManager = LocalFocusManager.current

    // Clear focus when the overlay activates (to prevent the IME staying open)
    LaunchedEffect(isActive) {
        if (isActive) {
            focusManager.clearFocus()
        }
    }

    AnimatedDialog(
        onDismissRequest = { /* Thou shalt not be dismissed */ },
        properties = AnimatedDialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isActive) {
                // Spinner
                CircularProgressIndicator()
                if (!loadingText.isNullOrBlank()) {
                    // Text below the spinner
                    Text(
                        loadingText,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 0.dp),
                    )
                }
            }
        }
    }
}
