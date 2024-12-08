package com.hypergonial.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.view.components.HomeComponent
import com.hypergonial.chat.view.components.LoginComponent
import com.hypergonial.chat.view.components.NotFoundComponent
import com.hypergonial.chat.view.components.RootComponent
import com.hypergonial.chat.view.ChatTheme
import com.hypergonial.chat.view.content.HomeContent
import com.hypergonial.chat.view.content.LoginContent
import com.hypergonial.chat.view.content.NotFoundContent

val LocalUsingDarkTheme = compositionLocalOf { false }

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun App(root: RootComponent) {
    val state by root.data.subscribeAsState()

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
                println("c.instance: ${c.instance}")

                when (val child = c.instance) {
                    is RootComponent.Child.LoginChild -> LoginContent(child.component)
                    is RootComponent.Child.HomeChild -> HomeContent(child.component)
                    is RootComponent.Child.NotFoundChild -> NotFoundContent(child.component)
                }
            }
        }

    }
}
