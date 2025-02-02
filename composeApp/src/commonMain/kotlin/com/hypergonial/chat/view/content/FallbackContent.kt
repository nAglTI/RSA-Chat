package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hypergonial.chat.view.components.FallbackMainComponent

@Composable
fun FallbackContent(component: FallbackMainComponent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("There appear to be no channels in this guild!")
        Button(onClick = { component.onChannelCreateClicked() }) { Text("Create a channel") }
    }
}
