package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User

open class Event

open class MessageEvent(val message: Message) : Event()

class HeartbeatAckEvent : Event()

class MessageCreateEvent(message: Message) : MessageEvent(message)

class MessageUpdateEvent(message: Message) : MessageEvent(message)

class MemberCreateEvent(val member: Member) : Event()

class MemberRemoveEvent(val id: Snowflake, val guildId: Snowflake) : Event()

class GuildCreateEvent(val guild: Guild, val channels: List<Channel>, val members: List<Member>) : Event()

class GuildRemoveEvent(val guild: Guild) : Event()

class ChannelCreateEvent(val channel: Channel) : Event()

class ChannelRemoveEvent(val channel: Channel) : Event()

class ReadyEvent(val user: User, val guilds: List<Guild>) : Event()
