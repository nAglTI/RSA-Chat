package com.hypergonial.chat.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** A base class for an actor that can receive messages and process them. */
abstract class Actor<MessageT> {
    private var channel = Channel<MessageT>(Channel.Factory.UNLIMITED, onBufferOverflow = BufferOverflow.DROP_LATEST)
    private var runningJob: Job? = null
    /** The scope the actor is running in. */
    protected var scope: CoroutineScope? = null

    private suspend fun handleMessages() {
        for (message in channel) {
            onMessage(message)
        }
    }

    /** Run the actor. Returns only when the actor is stopped or the scope it runs on is cancelled. */
    suspend fun run() = coroutineScope {
        if (runningJob != null) {
            runningJob?.cancel()
        }
        scope = this
        runningJob = launch { handleMessages() }
    }

    /** Stops the actor. It can be restarted and no messages will be lost. */
    fun stop() {
        runningJob?.cancel()
        runningJob = null
        scope = null
    }

    /** Sends a message to the actor. */
    fun sendMessage(message: MessageT) {
        channel.trySend(message)
    }

    /**
     * This callback is called every time the actor receives a new message.
     *
     * @param message The message to process.
     */
    protected abstract fun onMessage(message: MessageT)
}
