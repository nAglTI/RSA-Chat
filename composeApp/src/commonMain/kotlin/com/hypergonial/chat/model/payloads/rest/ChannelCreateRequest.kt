package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Request to create a channel.
 *
 * Belongs to: POST /api/v1/guilds/{guildId}/channels
 *
 * @param name The name of the channel.
 * @param type The type of the channel. Currently this is always "GUILD_TEXT".
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChannelCreateRequest(val name: String, @EncodeDefault(ALWAYS) val type: String = "GUILD_TEXT")
