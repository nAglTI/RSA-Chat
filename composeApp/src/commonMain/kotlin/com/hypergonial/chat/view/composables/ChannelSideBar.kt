package com.hypergonial.chat.view.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable

@Composable
fun ChannelSideBar() {
    Row {
        GuildsList()

        ChannelsList()
    }
}

@Composable
fun GuildsList() {
    LazyColumn {  }
}

@Composable
fun ChannelsList() {
    LazyColumn {  }
}
