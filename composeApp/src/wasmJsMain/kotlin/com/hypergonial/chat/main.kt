package com.hypergonial.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.hypergonial.chat.view.components.DefaultRootComponent
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

private const val KEY_SAVED_STATE = "SAVED_STATE"

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

/** Change the app's lifecycle based on the document's visibility. */
private fun LifecycleRegistry.attachToDocument() {
    val onVisibilityChanged = { if (document.hasFocus()) resume() else pause() }

    onVisibilityChanged()

    document.addEventListener(type = "visibilitychange", callback = { onVisibilityChanged() })
}

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {

    val colorScheme =
        when {
            useDarkTheme -> darkColorScheme
            else -> lightColorScheme
        }

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()
    val stateKeeper = StateKeeperDispatcher(savedState = localStorage[KEY_SAVED_STATE]?.decodeSerializableContainer())

    val root = DefaultRootComponent(ctx = DefaultComponentContext(lifecycle, stateKeeper))

    lifecycle.attachToDocument()

    window.onbeforeunload = {
        localStorage[KEY_SAVED_STATE] = stateKeeper.save().encodeToString()
        null
    }

    ComposeViewport(document.body!!) { AppTheme { App(root) } }
}

fun SerializableContainer.encodeToString(): String = Json.encodeToString(SerializableContainer.serializer(), this)

@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun String.decodeSerializableContainer(): SerializableContainer? =
    try {
        Json.decodeFromString(SerializableContainer.serializer(), this)
    } catch (e: Exception) {
        null
    }
