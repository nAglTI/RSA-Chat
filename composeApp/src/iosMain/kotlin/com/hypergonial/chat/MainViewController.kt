package com.hypergonial.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureIcon
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.hypergonial.chat.view.components.DefaultRootComponent
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState
import com.mmk.kmpnotifier.notification.NotifierManager
import platform.UIKit.UIViewController

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {

    val dynamicThemeState =
        rememberDynamicMaterialThemeState(
            seedColor = Color(104, 165, 39),
            isDark = useDarkTheme,
            isAmoled = false,
            style = PaletteStyle.TonalSpot,
        )

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        DynamicMaterialTheme(state = dynamicThemeState, animate = true, content = content)
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Suppress("FunctionNaming")
fun MainViewController(): UIViewController {
    val stateKeeper = StateKeeperDispatcher()
    val backDispatcher = BackDispatcher()
    NotifierManager.setLogger {
        Logger.withTag("NotifierManager").i(it)
    }

    val root =
        DefaultRootComponent(
            ctx =
                DefaultComponentContext(
                    lifecycle = ApplicationLifecycle(),
                    stateKeeper = stateKeeper,
                    backHandler = backDispatcher,
                )
        )

    return ComposeUIViewController {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            endEdgeEnabled = false,
            backIcon = { progress, _ ->
                PredictiveBackGestureIcon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, progress = progress)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            AppTheme { App(root) }
        }
    }
}
