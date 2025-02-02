package com.hypergonial.chat

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer that wraps another serializer and falls back to a default value if the deserialization fails.
 *
 * @param delegate The serializer to delegate to
 * @param default The default value to return if deserialization fails
 */
class FallbackSerializer<T>(private val delegate: KSerializer<T>, private val default: T) : KSerializer<T> {
    override val descriptor: SerialDescriptor = delegate.descriptor

    private val logger = KotlinLogging.logger {}

    override fun serialize(encoder: Encoder, value: T) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): T {
        return try {
            delegate.deserialize(decoder)
        } catch (e: kotlinx.serialization.SerializationException) {
            logger.warn(e) { "Failed to deserialize element, falling back to default value" }
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

    private val logger = KotlinLogging.logger {}

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
            logger.warn(e) { "Failed to deserialize element, falling back to default value" }
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

private fun Int.zpad(to: Int): String {
    return this.toString().padStart(to, '0')
}

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
suspend fun DrawerState.toggle() {
    if (isClosed) {
        open()
    } else {
        close()
    }
}
