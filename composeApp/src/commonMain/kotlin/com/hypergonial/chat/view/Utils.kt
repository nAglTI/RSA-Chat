package com.hypergonial.chat.view

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
    private val delegate: KSerializer<T>,
    private val default: T
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

/** Fall back to a default value if the deserialization fails
 *
 * @param default The default value to return if deserialization fails
 * */
fun <T>KSerializer<T>.withFallbackValue(default: T): KSerializer<T> = FallbackSerializer(this, default)
