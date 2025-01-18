package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.MemberCreateEvent
import com.hypergonial.chat.model.payloads.Member
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MEMBER_CREATE")
class MemberCreate(@SerialName("data") val member: Member) : GatewayMessage(), EventConvertable {
    override fun toEvent(): Event {
        return MemberCreateEvent(member)
    }
}
