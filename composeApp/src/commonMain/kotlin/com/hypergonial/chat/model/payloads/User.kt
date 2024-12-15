package com.hypergonial.chat.model.payloads

import kotlinx.datetime.Instant

sealed interface PartialUser {
    val id: Snowflake
    val username: String
    val displayName: String
    val avatarUrl: String?

    val createdAt: Instant
        get() = id.createdAt
}

open class User(
    override val id: Snowflake,
    override val username: String,
    override val displayName: String,
    override val avatarUrl: String? = null
) : PartialUser

class Member(id: Snowflake, name: String, displayName: String, avatarUrl: String? = null, val nickName: String) :
    User(id, name, displayName, avatarUrl)
