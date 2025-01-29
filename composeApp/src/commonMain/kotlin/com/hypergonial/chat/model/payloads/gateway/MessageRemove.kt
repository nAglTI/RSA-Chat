package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.MessageRemoveEvent
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MESSAGE_REMOVE")
class MessageRemove(@SerialName("data") val payload: MessageRemovePayload) : GatewayMessage(),
    EventConvertable {
    override fun toEvent(): Event {
        return MessageRemoveEvent(payload.id, payload.channelId, payload.guildId)
    }
}

@Serializable
data class MessageRemovePayload(
    val id: Snowflake,
    @SerialName("channel_id")
    val channelId: Snowflake,
    @SerialName("guild_id")
    val guildId: Snowflake
)
