package com.hypergonial.chat

import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
import com.hypergonial.chat.view.content.HomeContent
import com.hypergonial.chat.view.content.LoginContent
import com.hypergonial.chat.view.content.NotFoundContent
import com.hypergonial.chat.view.content.RegisterContent

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
        // Drop the focus if the user clicks anywhere on the screen that is not clickable
        Surface {
            Children(
                stack = root.stack,
                animation = predictiveBackAnimation(
                    backHandler = root.backHandler,
                    fallbackAnimation = stackAnimation(scale() + fade()),
                    onBack = root::onBackClicked,
                )
            ) { c ->
                when (val child = c.instance) {
                    is RootComponent.Child.LoginChild -> LoginContent(child.component)
                    is RootComponent.Child.HomeChild -> HomeContent(child.component)
                    is RootComponent.Child.RegisterChild -> RegisterContent(child.component)
                    is RootComponent.Child.NotFoundChild -> NotFoundContent(child.component)
                    else -> error("Unknown child: $child")
                }
            }
        }

    }
}
