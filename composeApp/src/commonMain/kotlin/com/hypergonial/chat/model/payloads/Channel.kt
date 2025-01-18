package com.hypergonial.chat.model.payloads

import kotlinx.serialization.Serializable

@Serializable
data class Channel(val id: Snowflake, val name: String, val guildId: Snowflake, val type: String)
