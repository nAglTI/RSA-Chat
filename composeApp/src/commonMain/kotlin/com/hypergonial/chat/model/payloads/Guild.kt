package com.hypergonial.chat.model.payloads

import com.hypergonial.chat.model.settings
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
) {
    val avatarUrl: String?
        get() = avatarHash?.let {
            "${settings.getApiSettings().objectStoreUrl}/guilds/$id/$it.${
                it.split("_").last()
            }"
        }
}
