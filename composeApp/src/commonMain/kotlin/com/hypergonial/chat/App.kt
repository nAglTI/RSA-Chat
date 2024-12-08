package com.hypergonial.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource

import chat.composeapp.generated.resources.Res
import chat.composeapp.generated.resources.compose_multiplatform
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.components.RootComponent
import com.hypergonial.chat.view.ChatTheme

val LocalUsingDarkTheme = compositionLocalOf { false }

@Composable
fun App(root: RootComponent) {
    val state by root.data.subscribeAsState()

    ChatTheme {
        Scaffold(Modifier.fillMaxSize()) { padding ->
            Column(Modifier.fillMaxWidth().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Down for maintenance")
            }
        }
    }
}
