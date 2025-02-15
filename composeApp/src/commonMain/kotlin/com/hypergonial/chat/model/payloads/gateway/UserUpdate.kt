package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.UserUpdateEvent
import com.hypergonial.chat.model.payloads.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("USER_UPDATE")
data class UserUpdate(@SerialName("data") val user: User) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event {
        return UserUpdateEvent(user)
    }
}
