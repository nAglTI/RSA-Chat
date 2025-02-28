package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.ChannelCreateEvent
import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.payloads.Channel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CHANNEL_CREATE")
class ChannelCreate(@SerialName("data") val channel: Channel) : GatewayMessage(), EventConvertible {
    override fun toEvent() = ChannelCreateEvent(channel)
}
