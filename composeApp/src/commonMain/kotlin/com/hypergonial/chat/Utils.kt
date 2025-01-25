package com.hypergonial.chat

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** A serializer that wraps another serializer and falls back to a default value if the deserialization fails.
 *
 * @param delegate The serializer to delegate to
 * @param default The default value to return if deserialization fails
 * */
class FallbackSerializer<T>(
    private val delegate: KSerializer<T>, private val default: T
) : KSerializer<T> {
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

/** A serializer that wraps another serializer and falls back to a default value
 * if the deserialization fails, or if a predicate is met.
 *
 * @param delegate The serializer to delegate to
 * @param default The default value to return if deserialization fails
 * @param predicate The predicate to check if the fallback should be used
 * */
class FallbackSerializerWithPredicate<T>(
    private val delegate: KSerializer<T>,
    private val default: T,
    private val predicate: () -> Boolean
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

/** Fall back to a default value if the deserialization fails
 *
 * @param default The default value to return if deserialization fails
 * */
fun <T> KSerializer<T>.withFallbackValue(default: T): KSerializer<T> =
    FallbackSerializer(this, default)

/** Fall back to a default value if the deserialization fails, or if a predicate is met
 *
 * @param default The default value to return if deserialization fails
 * @param predicate The predicate to check if the fallback should be used
 * */
fun <T> KSerializer<T>.withFallbackValue(default: T, predicate: () -> Boolean): KSerializer<T> =
    FallbackSerializerWithPredicate(this, default, predicate)

val IntRange.end: Int
    get() = last + 1

/** Get a sublist of the list from the given range
 *
 * @param range The range of elements to get
 * */
fun <T> MutableList<T>.subList(range: IntRange) = subList(range.first, range.end)

/** Remove a range of elements from the list
 *
 * @param range The range of elements to remove
 * */
fun <T> MutableList<T>.removeRange(range: IntRange) = subList(range).clear()

fun genNonce(): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..16).map { chars.random() }.joinToString("")
}

/** Sanitize the text in the chat bar. */
fun TextFieldValue.sanitized(): TextFieldValue {
    // TODO: If editor perf becomes a problem, consider doing the tab count and replacement in one pass
    val tabCount = this.text.count { it == '\t' }
    val text = this.text.replace("\t", "    ")
    val selection =
        TextRange(this.selection.start + 3 * tabCount, this.selection.end + 3 * tabCount)
    return this.copy(text = text, selection = selection)
}
