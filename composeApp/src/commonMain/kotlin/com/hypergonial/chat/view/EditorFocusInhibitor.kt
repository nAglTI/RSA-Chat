package com.hypergonial.chat.view

import com.hypergonial.chat.model.Actor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

sealed interface FocusInhibitorMessage {
    data class Acquire(val key: String) : FocusInhibitorMessage

    data class Release(val key: String) : FocusInhibitorMessage

    data class Query(val result: CompletableDeferred<Boolean>) : FocusInhibitorMessage
}

/**
 * This interface is responsible for registering and releasing the inhibition of editor auto-focus behaviour via global
 * key events.
 *
 * If the user presses a key and no other component is currently using the editor, the editor will automatically gain
 * focus.
 *
 * This interface allows components to inhibit this behaviour by acquiring and releasing focus for a given key.
 *
 * If no component is inhibiting this behaviour, the editor will automatically gain focus.
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

    override suspend fun isFree(): Boolean = true
}

/** The default implementation of [EditorFocusInhibitor]. */
@DelicateCoroutinesApi
class DefaultEditorFocusInhibitor : EditorFocusInhibitor, Actor<FocusInhibitorMessage>() {
    private val focusState = hashSetOf<String>()

    init {
        // Run as a global background service
        GlobalScope.launch { run() }
    }

    override fun onMessage(message: FocusInhibitorMessage) {
        when (message) {
            is FocusInhibitorMessage.Acquire -> {
                focusState.add(message.key)
            }
            is FocusInhibitorMessage.Release -> {
                focusState.remove(message.key)
            }
            is FocusInhibitorMessage.Query -> {
                message.result.complete(focusState.isEmpty())
            }
        }
    }

    override fun acquire(key: String) {
        sendMessage(FocusInhibitorMessage.Acquire(key))
    }

    override fun release(key: String) {
        sendMessage(FocusInhibitorMessage.Release(key))
    }

    override suspend fun isFree(): Boolean {
        val result = CompletableDeferred<Boolean>()
        sendMessage(FocusInhibitorMessage.Query(result))
        return result.await()
    }
}

/** The editor focus inhibitor instance of the application. */
expect val editorFocusInhibitor: EditorFocusInhibitor
