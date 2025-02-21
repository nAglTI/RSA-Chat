package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("START_TYPING")
class StartTyping(val data: StartTypingPayload) : GatewayMessage() {
    constructor(channelId: Snowflake) : this(StartTypingPayload(channelId))
}

@Serializable
data class StartTypingPayload(@SerialName("channel_id") val channelId: Snowflake)
