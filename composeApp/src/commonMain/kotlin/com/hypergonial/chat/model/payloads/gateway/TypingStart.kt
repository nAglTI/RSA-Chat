package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.TypingStartEvent
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TYPING_START")
class TypingStart(val data: TypingStartData) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event {
        return TypingStartEvent(data.channelId, data.userId)
    }
}

@Serializable
data class TypingStartData(
    @SerialName("channel_id") val channelId: Snowflake,
    @SerialName("user_id") val userId: Snowflake,
)
