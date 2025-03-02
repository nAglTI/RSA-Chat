package com.hypergonial.chat.model.payloads

import kotlin.jvm.JvmInline
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Epoch of 2023-01-01T00:00:00Z in milliseconds
private const val EPOCH: ULong = 1_672_531_200_000u

/**
 * Represents a snowflake identifier.
 *
 * See https://en.wikipedia.org/wiki/Snowflake_ID for more information.
 *
 * @param inner The snowflake as a ULong.
 */
@Serializable(with = SnowflakeSerializer::class)
@JvmInline
value class Snowflake(val inner: ULong) : Comparable<Snowflake> {

    constructor(value: Int) : this(value.toULong())

    override fun compareTo(other: Snowflake) = inner.compareTo(other.inner)

    override fun toString(): String = inner.toString()

    fun toULong(): ULong = inner

    /** The timestamp of the snowflake in milliseconds */
    val timestampMillis: ULong
        get() = (inner shr 22) + EPOCH

    val createdAt: Instant
        get() = Instant.fromEpochMilliseconds(timestampMillis.toLong())
}

// Serialize Snowflake as a string in the format of the inner ULong
// This is necessary because certain languages can't parse numbers this big (*cough* JavaScript)
private class SnowflakeSerializer : KSerializer<Snowflake> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Snowflake", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Snowflake) {
        encoder.encodeString(value.inner.toString())
    }

    override fun deserialize(decoder: Decoder): Snowflake {
        try {
            return Snowflake(decoder.decodeString().toULong())
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Snowflake must be a number")
        }
    }
}

fun String.toSnowflake(): Snowflake {
    return Snowflake(this.toULong())
}

fun ULong.toSnowflake(): Snowflake {
    return Snowflake(this)
}

fun Long.toSnowflake(): Snowflake {
    return Snowflake(this.toULong())
}
