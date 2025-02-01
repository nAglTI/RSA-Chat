package com.hypergonial.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureIcon
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.hypergonial.chat.view.components.DefaultRootComponent

private val lightColorScheme = lightColorScheme(
    primary = Color(0xFF476810),
    onPrimary = Color(0xFF476810),
    primaryContainer = Color(0xFFC7F089),
    onPrimaryContainer = Color(0xFFC7F089),
)
private val darkColorScheme = darkColorScheme(
    primary = Color(0xFFACD370),
    onPrimary = Color(0xFF213600),
    primaryContainer = Color(0xFF324F00),
    onPrimaryContainer = Color(0xFF324F00),
)

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit
) {

    val colorScheme = when {
        useDarkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme, content = content
        )
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Suppress("FunctionNaming")
fun MainViewController() {
    val stateKeeper = StateKeeperDispatcher()
    val backDispatcher = BackDispatcher()

    val root = DefaultRootComponent(
        ctx = DefaultComponentContext(
            lifecycle = ApplicationLifecycle(),
            stateKeeper = stateKeeper,
            backHandler = backDispatcher
        )
    )

    ComposeUIViewController {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            endEdgeEnabled = false,
            backIcon = { progress, _ ->
                PredictiveBackGestureIcon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    progress = progress,
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            AppTheme {App(root) } }
        }
}
