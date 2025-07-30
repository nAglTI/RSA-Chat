package com.hypergonial.chat.data.remote.server_updates

import com.hypergonial.chat.data.remote.ChatApiService
import com.hypergonial.chat.model.MessageCreateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// TODO events and auth logic
class LongPollingUpdatesProvider(
    private val apiService: ChatApiService
) : RealtimeUpdatesProvider {
    private val _events = MutableSharedFlow<ChatEvent>()
    override val events: Flow<ChatEvent> = _events
    private var pollingJob: Job? = null

    override fun start(channelId: String) {
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            var lastId: String? = null
            while (isActive) {
                try {
                    val newMessages = apiService.fetchMessages(channelId, lastId)
                    newMessages.forEach { msg ->
                        _events.emit(MessageCreateEvent(msg))
                        lastId = msg.id
                    }
                } catch (e: Exception) {
                    // простой бэкофф перед повторением
                    delay(3000)
                }
            }
        }
    }

    override fun stop() {
        pollingJob?.cancel()
    }
}
