package com.hypergonial.chat.model.payloads

import kotlinx.serialization.Serializable

@Serializable
data class Guild(val id: Snowflake, val name: String, val avatarHash: String? = null)
