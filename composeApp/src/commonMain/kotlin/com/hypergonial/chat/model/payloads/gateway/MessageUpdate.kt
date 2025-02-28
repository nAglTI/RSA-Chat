package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.MessageUpdateEvent
import com.hypergonial.chat.model.payloads.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MESSAGE_UPDATE")
class MessageUpdate(@SerialName("data") val message: Message) : GatewayMessage(), EventConvertible {
    override fun toEvent() = MessageUpdateEvent(message)
}
