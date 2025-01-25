package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.Serializable

@Serializable
data class ChannelCreateRequest(val name: String, val type: String = "GUILD_TEXT")
