package com.hypergonial.chat.model.payloads

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val EPOCH: ULong = 1_672_531_200_000u

@Serializable(with = SnowflakeSerializer::class)
data class Snowflake(val inner: ULong) : Comparable<Snowflake> {
    override fun compareTo(other: Snowflake): Int {
        return inner.compareTo(other.inner)
    }

    override fun toString(): String = inner.toString()

    /** The timestamp of the snowflake in milliseconds */
    val timestampMillis: ULong
        get() = (inner shr 22) + EPOCH

    val createdAt: Instant
        get() = Instant.fromEpochMilliseconds(timestampMillis.toLong())


}

// Serialize Snowflake as a string in the format of the inner ULong
private class SnowflakeSerializer : KSerializer<Snowflake> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Snowflake", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Snowflake) {
        encoder.encodeString(value.inner.toString())
    }

    override fun deserialize(decoder: Decoder): Snowflake {
        try {
            return Snowflake(decoder.decodeString().toULong())
        }
        catch (e: NumberFormatException) {
            throw IllegalArgumentException("Snowflake must be a number")
        }
    }
}
