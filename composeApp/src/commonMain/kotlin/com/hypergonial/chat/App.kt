package com.hypergonial.chat

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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

val LocalUsingDarkTheme = compositionLocalOf { false }

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun App(root: RootComponent) {
    ChatTheme {
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
