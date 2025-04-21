package com.hypergonial.chat.view

import co.touchlab.kermit.Logger
import com.hypergonial.chat.model.Actor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * This interface is responsible for registering and releasing the inhibition of editor auto-focus behaviour via global
 * key events.
 *
 * If the user presses a key and no other component is currently using the editor, the main editor will automatically
 * gain focus.
 *
 * This interface allows components to inhibit this behaviour by acquiring and releasing focus for a given key.
 *
 * If no component is inhibiting this behaviour, the main editor will automatically gain focus.
 *
 * Is this horribly over-engineered? Yes. Is it necessary? Also yes.
 */
interface EditorFocusInhibitor {
    /** Acquire the focus for the given key. Acquiring the same key multiple times will do nothing. */
    fun acquire(key: String)

    /**
     * Release the focus for the given key.
     *
     * Releasing the same key multiple times will do nothing.
     */
    fun release(key: String)

    /** Check if the editor is free to grab focus. */
    suspend fun isFree(): Boolean
}

/** For use on platforms that do not need to support editor focus inhibition. */
object NoOpEditorFocusInhibitor : EditorFocusInhibitor {
    override fun acquire(key: String) = Unit

    override fun release(key: String) = Unit

    override suspend fun isFree(): Boolean = false
}

sealed class InhibitorMessage {
    data class Acquire(val key: String) : InhibitorMessage()

    data class Release(val key: String) : InhibitorMessage()

    data class Query(val response: CompletableDeferred<Boolean>) : InhibitorMessage()
}

class EditorFocusInhibitorActor : Actor<InhibitorMessage>() {
    private val focusStates = hashSetOf<String>()

    override fun onMessage(message: InhibitorMessage) {
        when (message) {
            is InhibitorMessage.Acquire -> acquire(message.key)
            is InhibitorMessage.Release -> release(message.key)
            is InhibitorMessage.Query -> message.response.complete(isFree())
        }
    }

    private fun acquire(key: String) {
        focusStates.add(key)
    }

    private fun release(key: String) {
        focusStates.remove(key)
    }

    private fun isFree(): Boolean = focusStates.isEmpty()
}

class DefaultEditorFocusInhibitor : EditorFocusInhibitor {
    private val actor = EditorFocusInhibitorActor()

    suspend fun run() = actor.run()

    fun stop() = actor.stop()

    override fun acquire(key: String) {
        actor.sendMessage(InhibitorMessage.Acquire(key))
    }

    override fun release(key: String) {
        actor.sendMessage(InhibitorMessage.Release(key))
    }

    override suspend fun isFree(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        actor.sendMessage(InhibitorMessage.Query(deferred))
        try {
            return deferred.await()
        } catch (e: CancellationException) {
            Logger.w { "Query for editor focus inhibition was cancelled: $e" }
            return false
        }
    }
}

/** The editor focus inhibitor instance of the application. */
expect val editorFocusInhibitor: EditorFocusInhibitor
