package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.HeartbeatAckEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("HEARTBEAT_ACK")
data object HeartbeatAck : GatewayMessage(), EventConvertible {
    override fun toEvent() = HeartbeatAckEvent()
}
