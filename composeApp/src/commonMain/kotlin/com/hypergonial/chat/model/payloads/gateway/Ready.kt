package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.ReadState
import com.hypergonial.chat.model.ReadyEvent
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("READY")
class Ready(val data: ReadyPayload) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event =
        ReadyEvent(
            data.user,
            data.guilds,
            data.readStates.associate { it.channelId to ReadState(it.lastMessageId, it.lastReadMessageId) },
        )
}

@Serializable
data class ReadyPayload(
    val user: User,
    val guilds: List<Guild>,
    @SerialName("read_states") val readStates: List<ReadStatePayload>,
)

@Serializable
data class ReadStatePayload(
    @SerialName("channel_id") val channelId: Snowflake,
    @SerialName("last_read_message_id") val lastReadMessageId: Snowflake?,
    @SerialName("last_message_id") val lastMessageId: Snowflake?,
)
