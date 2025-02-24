package com.hypergonial.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Css
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Html
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Javascript
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import co.touchlab.kermit.Logger
import com.hypergonial.chat.model.Mime
import com.hypergonial.chat.view.components.subcomponents.MessageEntryComponent
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.util.encodeBase64
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/**
 * A container that when used as state, always causes a recomposition when changed, even if the value is the same. This
 * is particularly useful in LaunchedEffect.
 *
 * To determine if two effects were dispatched from the same place, you can use (==) on the containers.
 *
 * @param value The value to store in the container
 */
data class EffectContainer<T>(val value: T, private val dispatchId: Int = Random.nextInt())

/** Create a new effect container from the given value. */
fun <T> T.containAsEffect() = EffectContainer(this)

/**
 * A serializer that wraps another serializer and falls back to a default value if the deserialization fails.
 *
 * @param delegate The serializer to delegate to
 * @param default The default value to return if deserialization fails
 */
class FallbackSerializer<T>(private val delegate: KSerializer<T>, private val default: T) : KSerializer<T> {
    override val descriptor: SerialDescriptor = delegate.descriptor

    private val logger = Logger.withTag("FallbackSerializer")

    override fun serialize(encoder: Encoder, value: T) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): T {
        return try {
            delegate.deserialize(decoder)
        } catch (e: kotlinx.serialization.SerializationException) {
            logger.w(e) { "Failed to deserialize element, falling back to default value" }
            default
        }
    }
}

/**
 * A serializer that wraps another serializer and falls back to a default value if the deserialization fails, or if a
 * predicate is met.
 *
 * @param delegate The serializer to delegate to
 * @param default The default value to return if deserialization fails
 * @param predicate The predicate to check if the fallback should be used
 */
class FallbackSerializerWithPredicate<T>(
    private val delegate: KSerializer<T>,
    private val default: T,
    private val predicate: () -> Boolean,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = delegate.descriptor

    private val logger = Logger.withTag("FallbackSerializerWithPredicate")

    override fun serialize(encoder: Encoder, value: T) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): T {
        if (predicate()) {
            return default
        }

        return try {
            delegate.deserialize(decoder)
        } catch (e: kotlinx.serialization.SerializationException) {
            logger.w(e) { "Failed to deserialize element, falling back to default value" }
            default
        }
    }
}

/**
 * Fall back to a default value if the deserialization fails
 *
 * @param default The default value to return if deserialization fails
 */
fun <T> KSerializer<T>.withFallbackValue(default: T): KSerializer<T> = FallbackSerializer(this, default)

/**
 * Fall back to a default value if the deserialization fails, or if a predicate is met
 *
 * @param default The default value to return if deserialization fails
 * @param predicate The predicate to check if the fallback should be used
 */
fun <T> KSerializer<T>.withFallbackValue(default: T, predicate: () -> Boolean): KSerializer<T> =
    FallbackSerializerWithPredicate(this, default, predicate)

/** The value after the last element in the progression. */
val IntRange.end: Int
    get() = last + 1

/**
 * Get a sublist of the list from the given range
 *
 * @param range The range of elements to get
 */
fun <T> MutableList<T>.subList(range: IntRange) = subList(range.first, range.end)

/**
 * Remove a range of elements from the list
 *
 * @param range The range of elements to remove
 */
fun <T> MutableList<T>.removeRange(range: IntRange) = subList(range).clear()

/** Generate a session ID for a new session. */
fun genSessionId(): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (0..8).map { chars.random() }.joinToString("")
}

/** Generate a nonce for a new message. */
fun genNonce(sessionId: String): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return sessionId + "." + (0..16).map { chars.random() }.joinToString("")
}



object SettingsExt {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Get a serializable object from the settings
     *
     * @param key The key to get the object for
     * @return The object or null if it does not exist
     */
    inline fun <reified T>Settings.getSerializable(key: String): @Serializable T? {
        val value = this.getStringOrNull(key)

        return if (value.isNullOrEmpty()) null else json.decodeFromString(value)
    }

    /**
     * Set a serializable object in the settings
     *
     * @param key The key to set the object for
     * @param value The object to set
     */
    inline fun <reified T>Settings.setSerializable(key: String, value: @Serializable T) {
        val serialized = json.encodeToString(value)

        this.putString(key, serialized)
    }
}

/**
 * Returns the total number of messages in a list of message entry components.
 *
 * @return The total number of messages contained in the entries
 */
fun List<MessageEntryComponent>.totalMessageCount(): Int = this.sumOf { it.data.value.messages.size }

/**
 * Removes the first n messages from the list of message entry components.
 *
 * @param n The number of messages to remove
 */
fun MutableList<MessageEntryComponent>.removeFirstMessages(n: Int) {
    var count = n
    while (count > 0) {
        val first = this.firstOrNull() ?: break

        val messages = first.data.value.messages
        val toRemove = minOf(count, messages.size)
        messages.removeRange(0 until toRemove)
        if (messages.isEmpty()) {
            this.removeFirst()
        }
        count -= toRemove
    }
}

/**
 * Removes the last n messages from the list of message entry components.
 *
 * @param n The number of messages to remove
 */
fun MutableList<MessageEntryComponent>.removeLastMessages(n: Int) {
    var count = n
    while (count > 0) {
        val last = this.lastOrNull() ?: break

        val messages = last.data.value.messages
        val toRemove = minOf(count, messages.size)
        messages.removeRange(messages.size - toRemove until messages.size)
        if (messages.isEmpty()) {
            this.removeLast()
        }
        count -= toRemove
    }
}

/**
 * Append another list of entries to this list. If the criteria for merging are met, the entries on the boundary will be
 * merged.
 *
 * @param messages The list of entries to append
 */
fun MutableList<MessageEntryComponent>.appendMessages(messages: List<MessageEntryComponent>) {
    if (messages.isEmpty()) {
        return
    }

    val last = this.lastOrNull()

    if (last != null && last.author?.id == messages.firstOrNull()?.author?.id) {
        val newCreation = messages.first().firstMessage()?.data?.value?.createdAt
        val lastCreation = last.lastMessage()?.data?.value?.createdAt

        if (
            newCreation == null ||
                lastCreation == null ||
                newCreation - lastCreation > 5.minutes ||
                last.data.value.messages.size + messages.first().data.value.messages.size > 100
        ) {
            this.addAll(messages)
            return
        }

        last.data.value.messages.addAll(0, messages.first().data.value.messages)

        this.addAll(messages.drop(1))
    } else {
        this.addAll(messages)
    }
}

/**
 * Prepend another list of entries to this list. If the criteria for merging are met, the entries on the boundary will
 * be merged.
 *
 * @param messages The list of entries to prepend
 */
fun MutableList<MessageEntryComponent>.prependMessages(messages: List<MessageEntryComponent>) {
    if (messages.isEmpty()) {
        return
    }

    val first = this.firstOrNull()

    if (first != null && first.author?.id == messages.lastOrNull()?.author?.id) {
        val newCreation = messages.last().lastMessage()?.data?.value?.createdAt
        val firstCreation = first.firstMessage()?.data?.value?.createdAt

        if (
            newCreation == null ||
                firstCreation == null ||
                firstCreation - newCreation > 5.minutes ||
                first.data.value.messages.size + messages.last().data.value.messages.size > 100
        ) {
            this.addAll(0, messages)
            return
        }

        first.data.value.messages.addAll(messages.last().data.value.messages)
        this.addAll(0, messages.dropLast(1))
    } else {
        this.addAll(0, messages)
    }
}

fun <T> List<T>.forEachReversed(action: (T) -> Unit) {
    for (i in size - 1 downTo 0) {
        action(this[i])
    }
}

/** Sanitize the text in the chat bar. */
fun TextFieldValue.sanitized(): TextFieldValue {
    // TODO: If editor perf becomes a problem, consider doing the tab count and replacement in one
    // pass
    val tabCount = this.text.count { it == '\t' }
    val text = this.text.replace("\t", "    ")
    val selection = TextRange(this.selection.start + 3 * tabCount, this.selection.end + 3 * tabCount)
    return this.copy(text = text, selection = selection)
}

/**
 * Formats the instant to a human-readable format.
 *
 * @return The formatted date string
 */
fun Instant.toHumanReadable(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val date = this.toLocalDateTime(TimeZone.currentSystemDefault())

    return when (date.dayOfYear) {
        now.dayOfYear -> {
            "Today at ${date.hour.zpad(2)}:${date.minute.zpad(2)}"
        }
        now.dayOfYear - 1 -> {
            "Yesterday at ${date.hour.zpad(2)}:${date.minute.zpad(2)}"
        }
        now.dayOfYear + 1 -> {
            "Tomorrow at ${date.hour.zpad(2)}:${date.minute.zpad(2)}"
        }
        else -> {
            "${date.year.zpad(4)}-${date.monthNumber.zpad(2)}-${date.dayOfMonth.zpad(2)} at ${
                date.hour.zpad(
                    2
                )
            }:${date.minute.zpad(2)}"
        }
    }
}

private fun Int.zpad(to: Int): String = this.toString().padStart(to, '0')

/**
 * A tooltip position provider that tries to display the tooltip next to the anchor object, as opposed to above or below
 * it.
 *
 * @param spacingBetweenTooltipAndAnchor The spacing between the tooltip and the anchor object.
 */
@Composable
fun rememberHorizontalTooltipPositionProvider(spacingBetweenTooltipAndAnchor: Dp): PopupPositionProvider {
    val tooltipAnchorSpacing = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    return remember(tooltipAnchorSpacing) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                // Try to position on the right first
                var x = anchorBounds.right + tooltipAnchorSpacing

                // If it doesn't fit on the right, position on the left
                if (x + popupContentSize.width > windowSize.width) {
                    x = anchorBounds.left - popupContentSize.width - tooltipAnchorSpacing
                }

                // Center vertically relative to anchor
                val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2

                return IntOffset(x, y)
            }
        }
    }
}

/** Toggles the drawer state. If the drawer is closed, it will open it. If the drawer is open, it will close it. */
suspend fun DrawerState.toggle() = if (isClosed) open() else close()

/**
 * Ensures that the string has a slash at the end. If the String had a slash at the end, the original string is
 * returned.
 */
fun String.ensureSlashAtEnd(): String = if (!this.endsWith("/")) "$this/" else this

/**
 * Ensures that the string does not have a slash at the end. If the String had no slash at the end, the original string
 * is returned.
 */
fun String.ensureNoSlashAtEnd(): String = if (this.endsWith("/")) this.dropLast(1) else this

/** Gets the appropriate icon for the mime type. */
fun Mime.getIcon(): ImageVector {
    return when (this.type) {
        "image" -> Icons.Outlined.Image
        "video" -> Icons.Outlined.Movie
        "audio" -> Icons.Outlined.MusicNote
        "text" ->
            when (this.subtype) {
                "html" -> Icons.Outlined.Html
                "xhtml+xml" -> Icons.Outlined.Html
                "javascript" -> Icons.Outlined.Javascript
                "css" -> Icons.Outlined.Css
                "calendar" -> Icons.Outlined.CalendarMonth
                else -> Icons.Outlined.Description
            }
        "application" ->
            when (this.subtype) {
                "pdf" -> Icons.Outlined.PictureAsPdf
                "zip" -> Icons.Outlined.FolderZip
                "x-zip-compressed" -> Icons.Outlined.FolderZip
                "zip-compressed" -> Icons.Outlined.FolderZip
                "vnd.rar" -> Icons.Outlined.FolderZip
                "x-rar-compressed" -> Icons.Outlined.FolderZip
                "x-7z-compressed" -> Icons.Outlined.FolderZip
                "x-tar" -> Icons.Outlined.FolderZip
                "x-font-ttf" -> Icons.Outlined.FormatSize
                "vnd.android.package-archive" -> Icons.Outlined.Android
                "x-msdownload" -> Icons.Outlined.SettingsApplications
                "x-sh" -> Icons.Outlined.Terminal
                "x-shellscript" -> Icons.Outlined.Terminal
                else -> Icons.Outlined.FilePresent
            }
        "model" -> Icons.Outlined.ViewInAr
        else -> Icons.Outlined.FilePresent
    }
}

/** Trim a filename to a maximum of 20 characters. Keeps the file extension visible. */
fun String.trimFilename(): String {
    return if (length > 20) {
        val ext = substringAfterLast('.', missingDelimiterValue = "")

        substring(0, 15) + "{...}" + if (ext.isNotEmpty()) ".$ext" else ""
    } else this
}

/** Get the mime type of the file. */
fun PlatformFile.getMime(): Mime {
    return Mime.fromUrl(this.name) ?: Mime.default()
}

/**
 * Convert the file to a base64 encoded data URL.
 *
 * https://developer.mozilla.org/en-US/docs/Web/URI/Schemes/data
 */
suspend fun PlatformFile.toDataUrl(): String {
    return "data:${this.getMime()};base64,${this.readBytes().encodeBase64()}"
}

/**
 * A modifier that adds an alt-click listener to the component.
 *
 * On desktop platforms this registers a right-click, on mobile platforms this registers a long press.
 *
 * @param onClick The callback to call when the alt-click is detected
 */
@Composable expect fun Modifier.altClickable(onClick: () -> Unit): Modifier

private fun KeyEvent.isModifierGesture(key: Key): Boolean {
    if (this.type != KeyEventType.KeyDown || this.key != key) {
        return false
    }

    return if (platform.platformType == PlatformType.MAC) {
        this.isMetaPressed
    } else {
        this.isCtrlPressed
    }
}

fun KeyEvent.isCopyGesture(): Boolean = isModifierGesture(Key.C)

fun KeyEvent.isCutGesture(): Boolean = isModifierGesture(Key.X)

fun KeyEvent.isPasteGesture(): Boolean = isModifierGesture(Key.V)

/** Returns a sequence of files if the clipboard contains files. */
expect fun ClipboardManager.getFiles(): List<PlatformFile>?
