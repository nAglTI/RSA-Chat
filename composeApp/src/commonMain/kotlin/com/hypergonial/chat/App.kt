package com.hypergonial.chat

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.hypergonial.chat.view.ChatTheme
import com.hypergonial.chat.view.components.RootComponent

val LocalUsingDarkTheme = compositionLocalOf { false }

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun App(root: RootComponent) {
    setSingletonImageLoaderFactory { context -> ImageLoader.Builder(context).crossfade(true).build() }

    ChatTheme {
        Surface {
            Children(stack = root.stack, animation = platform.backAnimation(root.backHandler, root::onBackClicked)) {
                child ->
                child.instance.component.Display()
            }
        }
    }
}
