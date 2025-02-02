package com.hypergonial.chat.model.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A channel in a guild.
 *
 * @param id The ID of the channel.
 * @param name The name of the channel.
 * @param guildId The ID of the guild the channel is in.
 * @param type The type of the channel. Currently this is always "GUILD_TEXT".
 * */
@Serializable
data class Channel(
    val id: Snowflake,
    val name: String,
    @SerialName("guild_id")
    val guildId: Snowflake,
    val type: String
)
