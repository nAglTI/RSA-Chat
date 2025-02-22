package com.hypergonial.chat.view

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    suspend fun acquire(key: String)

    /**
     * Release the focus for the given key.
     *
     * Releasing the same key multiple times will do nothing.
     */
    suspend fun release(key: String)

    /** Check if the editor is free to grab focus. */
    suspend fun isFree(): Boolean
}

/** For use on platforms that do not need to support editor focus inhibition. */
object NoOpEditorFocusInhibitor : EditorFocusInhibitor {
    override suspend fun acquire(key: String) = Unit

    override suspend fun release(key: String) = Unit

    override suspend fun isFree(): Boolean = true
}

class DefaultEditorFocusInhibitor : EditorFocusInhibitor {
    private val lock = Mutex()
    private val focusStates = hashSetOf<String>()

    override suspend fun acquire(key: String) {
        lock.withLock { focusStates.add(key) }
    }

    override suspend fun release(key: String) {
        lock.withLock { focusStates.remove(key) }
    }

    override suspend fun isFree(): Boolean = lock.withLock { focusStates.isEmpty() }
}

/** The editor focus inhibitor instance of the application. */
expect val editorFocusInhibitor: EditorFocusInhibitor
