package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.MemberCreateEvent
import com.hypergonial.chat.model.payloads.Member
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MEMBER_CREATE")
class MemberCreate(@SerialName("data") val member: Member) : GatewayMessage(), EventConvertible {
    override fun toEvent() = MemberCreateEvent(member)
}
