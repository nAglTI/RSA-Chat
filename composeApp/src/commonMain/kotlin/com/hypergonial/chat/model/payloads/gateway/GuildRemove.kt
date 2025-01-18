package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.GuildRemoveEvent
import com.hypergonial.chat.model.payloads.Guild
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GUILD_REMOVE")
class GuildRemove(@SerialName("data") val guild: Guild) : GatewayMessage(), EventConvertable {
    override fun toEvent(): Event {
        return GuildRemoveEvent(guild)
    }
}
