package com.hypergonial.chat.model.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Guild(
    val id: Snowflake,
    val name: String,
    @SerialName("owner_id")
    val ownerId: Snowflake,
    @SerialName("avatar_hash")
    val avatarHash: String? = null
)
