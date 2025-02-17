package com.hypergonial.chat.view

import androidx.compose.ui.input.key.KeyEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class EventFlow<T> {
    private val inner =
        MutableSharedFlow<T>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    fun send(event: T) {
        if (inner.subscriptionCount.value == 0) {
            return
        }

        inner.tryEmit(event)
    }

    suspend fun receive(block: (T) -> Unit): Nothing = inner.collect(block)
}

typealias KeyEventFlow = EventFlow<KeyEvent>

val globalKeyEventFlow = KeyEventFlow()
