package com.hypergonial.chat

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.hypergonial.chat.view.components.RootComponent
import com.hypergonial.chat.view.ChatTheme
import com.hypergonial.chat.view.components.prompts.NewGuildComponent
import com.hypergonial.chat.view.content.ChannelContent
import com.hypergonial.chat.view.content.DebugSettingsContent
import com.hypergonial.chat.view.content.LoginContent
import com.hypergonial.chat.view.content.MainContent
import com.hypergonial.chat.view.content.NotFoundContent
import com.hypergonial.chat.view.content.RegisterContent
import com.hypergonial.chat.view.content.prompts.CreateChannelContent
import com.hypergonial.chat.view.content.prompts.CreateGuildContent
import com.hypergonial.chat.view.content.prompts.JoinGuildContent
import com.hypergonial.chat.view.content.prompts.NewGuildContent

const val IS_DEVELOPMENT_BUILD = true

val LocalUsingDarkTheme = compositionLocalOf { false }

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun App(root: RootComponent) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    ChatTheme {
        Surface {
            Children(
                stack = root.stack,
                animation = platform.backAnimation(root.backHandler, root::onBackClicked),
            ) { child -> child.instance.component.Display() }
        }

    }
}
