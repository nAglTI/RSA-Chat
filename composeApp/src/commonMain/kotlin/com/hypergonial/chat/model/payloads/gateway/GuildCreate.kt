package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.GuildCreateEvent
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GUILD_CREATE")
class GuildCreate(val data: GuildCreatePayload) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event {
        return GuildCreateEvent(data.guild, data.channels, data.members)
    }
}

@Serializable class GuildCreatePayload(val guild: Guild, val channels: List<Channel>, val members: List<Member>)
