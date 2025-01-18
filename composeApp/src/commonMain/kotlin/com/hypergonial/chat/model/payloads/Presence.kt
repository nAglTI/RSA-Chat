package com.hypergonial.chat.model.payloads

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PresenceSerializer::class)
enum class Presence(val value: String) {
    Online("ONLINE"),
    Away("AWAY"),
    Busy("BUSY"),
    Offline("OFFLINE")
}

class PresenceSerializer : KSerializer<Presence> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Presence", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Presence) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): Presence {
        return when (val value = decoder.decodeString()) {
            "ONLINE" -> Presence.Online
            "AWAY" -> Presence.Away
            "BUSY" -> Presence.Busy
            "OFFLINE" -> Presence.Offline
            else -> throw SerializationException("Unknown Presence value: $value")
        }
    }
}
