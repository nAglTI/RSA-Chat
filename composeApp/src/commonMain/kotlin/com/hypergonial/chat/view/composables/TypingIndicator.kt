package com.hypergonial.chat.view.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A typing indicator that animates three dots to simulate typing.
 *
 * @param dotSize The size of each dot.
 * @param dotSpacing The spacing between each dot.
 * @param bounceHeight The height of the bounce.
 * @param bounceDuration The duration of the bounce.
 * @param isPaused Whether the animation is paused.
 * @param initialColor The color of the dots when they are not bouncing.
 * @param bounceColor The color of the dots when they are bouncing.
 * @param dotCount The number of dots to animate.
 */
@Composable
fun TypingIndicator(
    dotSize: Dp = 7.dp,
    dotSpacing: Dp = 4.dp,
    bounceHeight: Dp = 8.dp,
    bounceDuration: Int = 350,
    isPaused: Boolean = false,
    initialColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f).compositeOver(Color.Gray),
    bounceColor: Color = MaterialTheme.colorScheme.onSurface,
    dotCount: Int = 3,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Convert bounceHeight to pixels
    val bounceHeightPx = with(density) { bounceHeight.toPx() }

    val dots = remember { List(dotCount) { Animatable(0f) } }

    // Iter dots
    LaunchedEffect(isPaused) {
        if (isPaused) {
            return@LaunchedEffect
        }

        val jobs = mutableListOf<Job>()

        while (true) {
            jobs.clear()

            dots.forEachIndexed { i, dot ->
                jobs.add(
                    scope.launch {
                        delay((i * (bounceDuration / 3)).toLong())
                        // Animate upwards (first half)
                        dot.animateTo(
                            targetValue = -bounceHeightPx,
                            animationSpec = tween(durationMillis = bounceDuration / 2, easing = FastOutSlowInEasing),
                        )
                        delay(bounceDuration.toLong() / 8)
                        // Animate back down (second half)
                        dot.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = bounceDuration / 2, easing = FastOutSlowInEasing),
                        )
                    }
                )
            }

            jobs.forEach { it.join() }

            delay(bounceDuration.toLong() * 3)
        }
    }

    Row(
        modifier = Modifier.height(bounceHeight * 2 + dotSize).padding(bottom = dotSize),
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.Bottom,
    ) {
        dots.forEach { animatable ->
            Box(
                modifier =
                    Modifier.size(dotSize)
                        .graphicsLayer { translationY = animatable.value }
                        .background(
                            color = initialColor.copy(alpha = 1 + animatable.value).compositeOver(bounceColor),
                            shape = CircleShape,
                        )
            )
        }
    }
}
