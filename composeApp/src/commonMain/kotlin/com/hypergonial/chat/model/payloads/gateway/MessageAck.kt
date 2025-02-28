package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.MessageAckEvent
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MESSAGE_ACK")
class MessageAck(@SerialName("data") val payload: MessageAckPayload) : GatewayMessage(), EventConvertible {
    override fun toEvent() = MessageAckEvent(payload.channelId, payload.messageId)
}

@Serializable
data class MessageAckPayload(
    @SerialName("channel_id") val channelId: Snowflake,
    @SerialName("message_id") val messageId: Snowflake,
)
