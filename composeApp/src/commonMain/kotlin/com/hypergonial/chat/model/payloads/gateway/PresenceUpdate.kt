package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.PresenceUpdateEvent
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PRESENCE_UPDATE")
class PresenceUpdate(val data: PresenceUpdatePayload) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event {
        return PresenceUpdateEvent(data.userId, data.presence)
    }
}

@Serializable
data class PresenceUpdatePayload(
    @SerialName("user_id")
    val userId: Snowflake,
    val presence: String,
)
