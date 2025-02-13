package com.hypergonial.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.essenty.statekeeper.SerializableContainer
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.altClickable(onClick: () -> Unit): Modifier {
    return this.onPointerEvent(PointerEventType.Press) {
        it.awtEventOrNull?.let { event ->
            // Right-click
            if (event.button == 3) {
                onClick()
            }
        }
    }
}

/**
 * A function that runs the given block on the UI thread
 *
 * @param block The block to run on the UI thread.
 * @return The result of the block.
 */
@Suppress("TooGenericExceptionCaught")
internal fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    var error: Throwable? = null
    var result: T? = null

    SwingUtilities.invokeAndWait {
        try {
            result = block()
        } catch (e: Throwable) {
            error = e
        }
    }

    error?.also { throw it }

    @Suppress("UNCHECKED_CAST")
    return result as T
}

/**
 * Write a serializable container to a Java file handle
 *
 * @param file The file to write the serializable container to.
 */
@OptIn(ExperimentalSerializationApi::class)
fun SerializableContainer.writeToFile(file: File) {
    file.outputStream().use { output -> Json.encodeToStream(SerializableContainer.serializer(), this, output) }
}

/**
 * Read a serializable container from a Java file handle
 *
 * @return The serializable container read from the file, or null if the file does not exist or an error occurs.
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun File.readToSerializableContainer(): SerializableContainer? =
    takeIf(File::exists)?.inputStream()?.use { input ->
        try {
            Json.decodeFromStream(SerializableContainer.serializer(), input)
        } catch (e: Exception) {
            null
        }
    }
