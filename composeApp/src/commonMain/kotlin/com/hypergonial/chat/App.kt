package com.hypergonial.chat

import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.hypergonial.chat.domain.crypto.CryptoManager.testLibsodium
import com.hypergonial.chat.view.ChatTheme
import com.hypergonial.chat.view.components.RootComponent
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val LocalUsingDarkTheme = compositionLocalOf { false }

@Composable
fun App(root: RootComponent) {
    CoroutineScope(Dispatchers.Default).launch {
        LibsodiumInitializer.initialize()
    }

    LibsodiumInitializer.initializeWithCallback {
        testLibsodium()
    }
    setSingletonImageLoaderFactory { context ->
        // Fetches images on IO dispatcher by default
        ImageLoader.Builder(context).crossfade(true).build()
    }

    ChatTheme {
        Surface(Modifier.clickable(null, null) { /* Drop focus when unclickable things are clicked */ }) {
            Children(stack = root.stack, animation = platform.backAnimation(root.backHandler, root::onBackClicked)) {
                child ->
                child.instance.component.Display()
            }
        }
    }
}
