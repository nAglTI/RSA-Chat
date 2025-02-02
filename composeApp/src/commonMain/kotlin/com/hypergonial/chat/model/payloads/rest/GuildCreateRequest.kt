package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.Serializable

/**
 * Request to create a guild.
 *
 * Belongs to: POST /api/v1/guilds
 *
 * @param name The name of the guild.
 */
@Serializable data class GuildCreateRequest(val name: String)
