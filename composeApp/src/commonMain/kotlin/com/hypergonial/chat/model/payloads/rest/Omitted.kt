package com.hypergonial.chat.model.payloads.rest

import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = OmittedSerializer::class)
sealed interface OmittedOr<out T> {
    data class Present<T>(var value: T) : OmittedOr<T>

    data object Omitted : OmittedOr<Nothing>

    companion object {
        operator fun <T> invoke(value: T): OmittedOr<T> = Present(value)
    }

    fun getOrNull(): T? =
        when (this) {
            is Present -> value
            Omitted -> null
        }
}

fun <V : Any> KMutableProperty0<OmittedOr<V>>.delegate(): ReadWriteProperty<Any?, V?> =
    object : ReadWriteProperty<Any?, V?> {

        override fun getValue(thisRef: Any?, property: KProperty<*>): V? {
            return when (val omittedOr = this@delegate.get()) {
                is OmittedOr.Present -> omittedOr.value
                is OmittedOr.Omitted -> null
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
            val omittedOr = if (value == null) OmittedOr.Omitted else OmittedOr.Present(value)

            this@delegate.set(omittedOr)
        }
    }

@JsName("nullableDelegate")
@JvmName("nullableDelegate")
fun <V : Any> KMutableProperty0<OmittedOr<V?>>.delegate(): ReadWriteProperty<Any?, V?> =
    object : ReadWriteProperty<Any?, V?> {

        override fun getValue(thisRef: Any?, property: KProperty<*>): V? {
            return this@delegate.get().getOrNull()
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
            this@delegate.set(OmittedOr(value))
        }
    }

typealias Present<T> = OmittedOr.Present<T>

typealias Omitted = OmittedOr.Omitted

inline fun <T : Any, U : Any> OmittedOr<T>.map(mapper: (T) -> U): OmittedOr<U> =
    when (this) {
        is Omitted -> this
        is Present -> Present(mapper(value))
    }

@JsName("nullableMap")
@JvmName("nullableMap")
inline fun <T : Any, U : Any> OmittedOr<T?>.map(mapper: (T) -> U): OmittedOr<U?> =
    when (this) {
        is Omitted -> this
        is Present -> Present(value?.let { mapper(it) })
    }

class OmittedSerializer<T>(private val valueSerializer: KSerializer<T>) : KSerializer<OmittedOr<T>> {
    override val descriptor = valueSerializer.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: OmittedOr<T>) {
        when (value) {
            is OmittedOr.Present -> encoder.encodeNullableSerializableValue(valueSerializer, value.value)
            OmittedOr.Omitted -> encoder.encodeNull()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): OmittedOr<T> {
        val value = decoder.decodeNullableSerializableValue(valueSerializer)
        return if (value != null) OmittedOr.Present(value) else OmittedOr.Omitted
    }
}
