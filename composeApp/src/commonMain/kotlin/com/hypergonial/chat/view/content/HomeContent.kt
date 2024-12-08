package com.hypergonial.chat.view.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hypergonial.chat.view.components.HomeComponent

@Composable
fun HomeContent(component: HomeComponent) {
    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {
            Text("Home")
            Button(onClick = { component.onLogout() }) {
                Text("Logout")
            }
        }
    }
}
