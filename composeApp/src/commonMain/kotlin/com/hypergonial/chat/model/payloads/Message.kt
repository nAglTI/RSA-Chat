package com.hypergonial.chat.model.payloads

import kotlinx.datetime.Instant

class Message(val id: Snowflake, val content: String? = null, val author: PartialUser) {
    val createdAt: Instant
        get() = id.createdAt
}
