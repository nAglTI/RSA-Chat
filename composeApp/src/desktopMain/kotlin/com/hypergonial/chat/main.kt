package com.hypergonial.chat

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.hypergonial.chat.view.components.DefaultRootComponent
import com.hypergonial.chat.view.composables.Material3ContextMenuRepresentation
import java.awt.Dimension
import java.io.File

private const val SAVED_STATE_FILE_NAME = "state.dat"

private val lightColorScheme =
    lightColorScheme(
        primary = Color(0xFF476810),
        onPrimary = Color(0xFF476810),
        primaryContainer = Color(0xFFC7F089),
        onPrimaryContainer = Color(0xFFC7F089),
    )
private val darkColorScheme =
    darkColorScheme(
        primary = Color(0xFFACD370),
        onPrimary = Color(0xFF213600),
        primaryContainer = Color(0xFF324F00),
        onPrimaryContainer = Color(0xFF324F00),
    )

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {

    val colorScheme =
        when {
            useDarkTheme -> darkColorScheme
            else -> lightColorScheme
        }

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        CompositionLocalProvider(LocalContextMenuRepresentation provides Material3ContextMenuRepresentation()) {
            MaterialTheme(colorScheme = colorScheme, content = content)
        }
    }
}

fun main() {
    Logger.setLogWriters(Slf4jLogWriter())
    val lifecycle = LifecycleRegistry()
    // Deserialize state from state file
    val stateKeeper = StateKeeperDispatcher((File(SAVED_STATE_FILE_NAME).readToSerializableContainer()))

    val root = runOnUiThread { DefaultRootComponent(ctx = DefaultComponentContext(lifecycle, stateKeeper)) }

    application {
        val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)
        LifecycleController(lifecycle, windowState)

        Window(
            onCloseRequest = {
                stateKeeper.save().writeToFile(File(SAVED_STATE_FILE_NAME))
                exitApplication()
            },
            title = "Chat",
            state = windowState,
        ) {
            window.minimumSize = Dimension(300, 600)

            AppTheme { App(root) }
        }
    }
}
