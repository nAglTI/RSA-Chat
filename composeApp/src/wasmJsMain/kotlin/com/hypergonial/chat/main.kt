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
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

private const val KEY_SAVED_STATE = "SAVED_STATE"

/** Change the app's lifecycle based on the document's visibility. */
private fun LifecycleRegistry.attachToDocument() {
    val onVisibilityChanged = { if (document.hasFocus()) resume() else pause() }

    onVisibilityChanged()

    document.addEventListener(type = "visibilitychange", callback = { onVisibilityChanged() })
}

@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {


    val dynamicThemeState = rememberDynamicMaterialThemeState(
        seedColor = Color(104, 165, 39),
        isDark = useDarkTheme,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
    )

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        DynamicMaterialTheme(state = dynamicThemeState, animate = true, content = content)
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
