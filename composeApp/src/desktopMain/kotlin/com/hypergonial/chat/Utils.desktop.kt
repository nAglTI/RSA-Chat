package com.hypergonial.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.ClipboardManager
import co.touchlab.kermit.Logger
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.hypergonial.chat.model.Mime
import io.github.vinceglb.filekit.core.PlatformFile
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.net.URLConnection

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

/** Returns a sequence of files if the clipboard contains files. */
@Suppress("TooGenericExceptionCaught")
actual fun ClipboardManager.getFiles(): List<PlatformFile>? {
    return try {
        val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard
        val contents = systemClipboard.getContents(null) ?: return null

        // If the clipboard has files
        if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            @Suppress("UNCHECKED_CAST")
            val fileList = contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
            return fileList?.map { file -> PlatformFile(file) }
        }

        // If the clipboard is holding an image in memory
        if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            val image = contents.getTransferData(DataFlavor.imageFlavor) as? BufferedImage ?: return null
            val tempFile = File.createTempFile("image", ".png")
            tempFile.writeBytes(image.toBytes())
            return listOf(PlatformFile(tempFile))
        }
        return null
    } catch (e: Exception) {
        Logger.w { "Failed to copy file(s) from clipboard: ${e.message}" }
        null
    }
}

fun BufferedImage.toBytes(): ByteArray {
    ByteArrayOutputStream().use {
        ImageIO.write(this, "png", it)
        it.flush()
        return it.toByteArray()
    }
}
