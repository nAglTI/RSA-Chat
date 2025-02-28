package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.MemberRemoveEvent
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MEMBER_REMOVE")
class MemberRemove(val data: MemberRemovePayload) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event = MemberRemoveEvent(data.id, data.guildId)
}

@Serializable data class MemberRemovePayload(val id: Snowflake, @SerialName("guild_id") val guildId: Snowflake)
