package com.hypergonial.chat.view.content

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.components.HomeComponent

@Composable
fun HomeContent(component: HomeComponent) {
    val state by component.data.subscribeAsState()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }

    if (state.isLoading) {
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.hasGuilds) {
            Text("Welcome back!", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Select a guild from the sidebar to start chatting!",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 50.dp),
            )
        } else {
            Text("Welcome to Chat!", style = MaterialTheme.typography.headlineSmall)
            Text(
                "It looks like you're not part of any guilds yet.\n" +
                    "${if (platform.isDesktopOrWeb()) "Click" else "Tap"} the green + in the sidebar to join or create one!",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 50.dp),
            )
        }

        if (isSmall && !platform.isDesktopOrWeb()) {
            AnimatedArrow()
        }
    }
}

@Composable
private fun AnimatedArrow() {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetX by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(animation = tween(2500), repeatMode = RepeatMode.Restart),
        )

    // Compute alpha: fade in for the first 20 and fade out for the last 20.
    val currentAlpha =
        when {
            offsetX < 20f -> offsetX / 20f
            offsetX > 80f -> (100f - offsetX) / 20f
            else -> 1f
        } / 2f
    val arrowColor = MaterialTheme.colorScheme.onBackground.copy(alpha = currentAlpha)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Icon(
            imageVector = Icons.Filled.TouchApp,
            contentDescription = "Swipe to open sidebar",
            modifier = Modifier.size(80.dp).offset(x = (offsetX - 50f).dp).rotate(-30f),
            tint = arrowColor,
        )
    }
}
