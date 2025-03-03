package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Snowflake
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * A typing indicator for a user in a channel.
 *
 * @param userId The user that is typing.
 * @param lastUpdated The last time this typing indicator was updated.
 */
data class TypingIndicator(val userId: Snowflake, val lastUpdated: Instant) {
    /**
     * A typing indicator is "active" when it was updated less than 5 seconds ago.
     *
     * If true, it means that there is no need to resend this typing indicator to the server to maintain the typing
     * status of the user.
     */
    fun isActive() = Clock.System.now() - lastUpdated <= 5.seconds
}
