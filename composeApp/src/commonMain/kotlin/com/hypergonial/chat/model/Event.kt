package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User

open class Event

/** Event that is only used inside the application, and is not coming from the gateway directly */
open class InternalEvent : Event()

open class MessageEvent(val message: Message) : Event()

class HeartbeatAckEvent : Event()

/** Event dispatched when a message is created */
class MessageCreateEvent(message: Message) : MessageEvent(message)

/** Event dispatched when a message is edited */
class MessageUpdateEvent(message: Message) : MessageEvent(message)

/** Event dispatched when a member is created */
class MemberCreateEvent(val member: Member) : Event()

/** Event dispatched when a member is removed */
class MemberRemoveEvent(val id: Snowflake, val guildId: Snowflake) : Event()

/** Event dispatched when a guild is created */
class GuildCreateEvent(val guild: Guild, val channels: List<Channel>, val members: List<Member>) : Event()

/** Event dispatched when a guild is removed */
class GuildRemoveEvent(val guild: Guild) : Event()

/** Event dispatched when a channel is created */
class ChannelCreateEvent(val channel: Channel) : Event()

/** Event dispatched when a channel is removed */
class ChannelRemoveEvent(val channel: Channel) : Event()

/** Event dispatched after the client has authenticated with the gateway */
class ReadyEvent(val user: User, val guilds: List<Guild>) : Event()

class PresenceUpdateEvent(val userId: Snowflake, val presence: String) : Event()

enum class InvalidationReason {
    Normal,
    AuthenticationFailure,
    Timeout,
}

/** Event dispatched when the session is invalidated
 *
 * Listeners should clear all state and return to the login screen
 */
class SessionInvalidatedEvent(val reason: InvalidationReason) : InternalEvent()

/** Internal event dispatched when the application should bring a channel into focus */
class FocusChannelEvent(val channel: Channel) : InternalEvent()

/** Internal event dispatched when the application should bring a guild into focus */
class FocusGuildEvent(val guild: Guild) : InternalEvent()
