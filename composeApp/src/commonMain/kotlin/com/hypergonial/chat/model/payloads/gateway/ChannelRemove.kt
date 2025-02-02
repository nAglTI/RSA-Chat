package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.ChannelRemoveEvent
import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.payloads.Channel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CHANNEL_REMOVE")
class ChannelRemove(@SerialName("data") val channel: Channel) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event {
        return ChannelRemoveEvent(channel)
    }
}
