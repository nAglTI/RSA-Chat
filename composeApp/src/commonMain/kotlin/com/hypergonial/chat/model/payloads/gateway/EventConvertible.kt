package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event

/** A common interface for objects that can be converted to an event. */
sealed interface EventConvertible {
    fun toEvent(): Event
}
