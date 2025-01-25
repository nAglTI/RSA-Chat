package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.ReadyEvent
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("READY")
class Ready(val data: ReadyPayload) : GatewayMessage(), EventConvertable {
    override fun toEvent(): Event {
        return ReadyEvent(data.user, data.guilds)
    }
}

@Serializable
data class ReadyPayload(
    val user: User,
    val guilds: List<Guild>
)
