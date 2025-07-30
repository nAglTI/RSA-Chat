package com.hypergonial.chat.data.remote.server_updates

import kotlinx.coroutines.flow.Flow

// todo events
interface RealtimeUpdatesProvider {
    fun start(channelId: String)
    fun stop()
    val events: Flow<ChatEvent>
}
