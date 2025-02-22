package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.datetime.Instant

/**
 * A typing indicator for a user in a channel.
 *
 * @param userId The user that is typing.
 * @param lastUpdated The last time this typing indicator was updated.
 */
data class TypingIndicator(val userId: Snowflake, val lastUpdated: Instant)
