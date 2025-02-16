package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.Serializable

/**
 * Represents the last opened guild and channels of the user.
 *
 * @param lastOpenGuild The last opened guild.
 * @param lastOpenChannels The last opened channels of the user. A mapping of guild to channel.
 */
@Serializable
data class LastOpenedPrefs(val lastOpenGuild: Snowflake?, val lastOpenChannels: HashMap<Snowflake, Snowflake>) {
    companion object {
        fun default() = LastOpenedPrefs(null, hashMapOf())
    }
}
