package com.hypergonial.chat.view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A full-screen progress indicator that can be displayed over the content, blocking user interaction.
 *
 * @param isActive Whether the progress indicator should be displayed
 * If false, the progress indicator will not be displayed and the content will be displayed as normal
 * @param loadingText The text to display below the progress indicator
 * @param content The content to display behind the progress indicator
 */
@Composable
fun FullScreenProgressIndicator(
    isActive: Boolean, loadingText: String? = null, content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        // Semi-transparent box to dim the content behind the spinner
        AnimatedVisibility(visible = isActive, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().blur(30.dp).pointerInput(Unit) {
                    // Disable touch events
                }.background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                ),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isActive) {
                // Spinner
                CircularProgressIndicator()
                if (!loadingText.isNullOrBlank()) {
                    // Text below the spinner
                    Text(
                        loadingText,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 0.dp)
                    )
                }
            }
        }
    }
}
