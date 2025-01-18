package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event

sealed interface EventConvertable {
    fun toEvent(): Event
}
