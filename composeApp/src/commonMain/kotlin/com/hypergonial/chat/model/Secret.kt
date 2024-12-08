package com.hypergonial.chat.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Wrapper for a secret value to ensure it doesn't accidentally get printed */
@Serializable(with = SecretSerializer::class)
data class Secret<T> (private val inner: T) {
    fun expose(): T = inner

    override fun toString(): String {
        return "***************"
    }

    fun map(f: (T) -> T): Secret<T> {
        return Secret(f(inner))
    }
}

class SecretSerializer<T>(private val innerSerializer: KSerializer<T>) : KSerializer<Secret<T>> {
    override val descriptor: SerialDescriptor = innerSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Secret<T>) {
        innerSerializer.serialize(encoder, value.expose())
    }

    override fun deserialize(decoder: Decoder): Secret<T> {
        return Secret(innerSerializer.deserialize(decoder))
    }
}
