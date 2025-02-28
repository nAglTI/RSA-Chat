package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.GuildUpdateEvent
import com.hypergonial.chat.model.payloads.Guild
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GUILD_UPDATE")
class GuildUpdate(@SerialName("data") val guild: Guild) : GatewayMessage(), EventConvertible {
    override fun toEvent() = GuildUpdateEvent(guild)
}
