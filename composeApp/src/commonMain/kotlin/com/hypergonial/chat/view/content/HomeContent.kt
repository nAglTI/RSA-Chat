package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hypergonial.chat.view.components.HomeComponent

@Composable
fun HomeContent(component: HomeComponent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("Welcome to Chat!")
        Text(
            "It looks like you're not part of any guilds yet. Click the green + in the sidebar to join or create one!"
        )
        // TODO: Add button here to join guilds as well?
    }
}
