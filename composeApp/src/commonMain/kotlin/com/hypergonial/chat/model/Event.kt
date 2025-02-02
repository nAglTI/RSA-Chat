package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User

/** Base class for all events dispatched by the application */
open class Event

/** Base class for events that are only used inside the application, and are not coming from the gateway directly */
open class InternalEvent : Event()

/** Base class for events dispatched when a message related event is received from the gateway. */
open class MessageEvent(val message: Message) : Event()

/** Event dispatched when the gateway acknowledges a heartbeat sent by the client. */
class HeartbeatAckEvent : Event()

/** Event dispatched when a message is created */
class MessageCreateEvent(message: Message) : MessageEvent(message)

/** Event dispatched when a message is edited */
class MessageUpdateEvent(message: Message) : MessageEvent(message)

/** Event dispatched when a message is deleted */
class MessageRemoveEvent(val id: Snowflake, val channelId: Snowflake, val guildId: Snowflake) : Event()

/** Event dispatched when a member is created */
class MemberCreateEvent(val member: Member) : Event()

/** Event dispatched when a member is removed */
class MemberRemoveEvent(val id: Snowflake, val guildId: Snowflake) : Event()

/** Event dispatched when a guild is created */
class GuildCreateEvent(val guild: Guild, val channels: List<Channel>, val members: List<Member>) : Event()

/** Event dispatched when a guild is updated */
class GuildUpdateEvent(val guild: Guild) : Event()

/** Event dispatched when a guild is removed */
class GuildRemoveEvent(val guild: Guild) : Event()

/** Event dispatched when a channel is created */
class ChannelCreateEvent(val channel: Channel) : Event()

/** Event dispatched when a channel is removed */
class ChannelRemoveEvent(val channel: Channel) : Event()

/** Event dispatched after the client has authenticated with the gateway */
class ReadyEvent(val user: User, val guilds: List<Guild>) : Event()

/** Event dispatched when a user's presence is updated */
class PresenceUpdateEvent(val userId: Snowflake, val presence: String) : Event()

/** Event dispatched when a user is logged in */
class LoginEvent : InternalEvent()

/** Event dispatched when a user is logged out */
class LogoutEvent : InternalEvent()

/** The reason for a gateway session being invalidated */
enum class InvalidationReason {
    Normal,
    AuthenticationFailure,
    Timeout,
    Abnormal,
}

/** Event dispatched when the session is invalidated
 *
 * Listeners should clear all state and return to the login screen in case of a non-normal invalidation.
 */
class SessionInvalidatedEvent(val reason: InvalidationReason) : InternalEvent()

/** Internal event dispatched when the application should bring a channel into focus */
class FocusChannelEvent(val channel: Channel) : InternalEvent()

/** Internal event dispatched when the application should bring a guild into focus */
class FocusGuildEvent(val guild: Guild) : InternalEvent()
