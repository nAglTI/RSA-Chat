package com.hypergonial.chat.model.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: Snowflake,
    val name: String,
    @SerialName("guild_id")
    val guildId: Snowflake,
    val type: String
)
