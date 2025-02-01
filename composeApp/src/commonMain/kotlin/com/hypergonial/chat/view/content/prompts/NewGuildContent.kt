package com.hypergonial.chat.view.content.prompts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.components.prompts.NewGuildComponent

@Composable
fun NewGuildContent(component: NewGuildComponent) {
    Box {
        if (platform.needsBackButton()) {
            IconButton(
                onClick = { component.onBackClicked() },
                modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp).align(Alignment.TopStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Button(onClick = { component.onGuildCreateClicked() }) {
                Text("Create")
            }
            Button(onClick = { component.onGuildJoinClicked() }) {
                Text("Join")
            }
        }
    }


}
