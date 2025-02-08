package com.hypergonial.chat.model.payloads

import com.hypergonial.chat.ensureNoSlashAtEnd
import com.hypergonial.chat.model.settings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A guild.
 *
 * @param id The ID of the guild.
 * @param name The name of the guild.
 * @param ownerId The ID of the owner of the guild.
 * @param avatarHash The hash of the guild's avatar.
 */
@Serializable
data class Guild(
    val id: Snowflake,
    val name: String,
    @SerialName("owner_id") val ownerId: Snowflake,
    @SerialName("avatar_hash") val avatarHash: String? = null,
) {
    val avatarUrl: String?
        get() =
            avatarHash?.let {
                "${settings.getApiSettings().objectStoreUrl.ensureNoSlashAtEnd()}/guilds/$id/$it.${
                it.split("_").last()
            }"
            }
}
