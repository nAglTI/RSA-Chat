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

/** Event dispatched when the gateway acknowledges a heartbeat sent by the client. */
class HeartbeatAckEvent : Event()

/** Event dispatched when a message is created */
class MessageCreateEvent(val message: Message) : Event()

/** Event dispatched when a message is edited */
class MessageUpdateEvent(val message: Message) : Event()

/** Event dispatched when a message is deleted */
class MessageRemoveEvent(val id: Snowflake, val channelId: Snowflake, val guildId: Snowflake) : Event()

/** Event dispatched when a user starts typing in a channel */
class TypingStartEvent(val channelId: Snowflake, val userId: Snowflake) : Event()

/**
 * Event dispatched when a user stops typing in a channel.
 *
 * This is always dispatched before a [MessageCreateEvent] when a user sends a message.
 *
 * This is an internal event, it's not dispatched by the gateway directly.
 */
class TypingEndEvent(val channelId: Snowflake, val userId: Snowflake) : InternalEvent()

/** Event dispatched when messages were read until messageId by a session of the currently authenticated user. */
class MessageAckEvent(val channelId: Snowflake, val messageId: Snowflake) : Event()

/** Event dispatched when a user is updated */
class UserUpdateEvent(val user: User) : Event()

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
class ReadyEvent(
    val user: User,
    val guilds: List<Guild>,
    val readStates: Map<Snowflake, ReadState>,
    val wasReconnect: Boolean = false,
) : Event()

data class ReadState(val lastMessageId: Snowflake?, val lastReadMessageId: Snowflake?)

/** Event dispatched when a user's presence is updated */
class PresenceUpdateEvent(val userId: Snowflake, val presence: String) : Event()

/** Event dispatched when a user is logged in */
class LoginEvent : InternalEvent()

/** Event dispatched when a user is logged out */
class LogoutEvent : InternalEvent()

/** Event dispatched when the upload status of a set of attachments changes */
class UploadProgressEvent(val nonce: String, val completionRate: Double) : InternalEvent()

/** The reason for a gateway session being invalidated */
enum class InvalidationReason {
    Normal,
    AuthenticationFailure,
    Timeout,
    Abnormal,
}

/**
 * Event dispatched when the session is invalidated
 *
 * Listeners should clear all state and return to the login screen in case of a non-normal invalidation.
 */
class SessionInvalidatedEvent(val reason: InvalidationReason, val willReconnect: Boolean = false) : InternalEvent()

/** Internal event dispatched when the application should bring a channel into focus */
class FocusChannelEvent(val channel: Channel) : InternalEvent()

/** Internal event dispatched when the application should bring a guild into focus */
class FocusGuildEvent(val guild: Guild) : InternalEvent()

/** Internal event dispatched when the application should bring an asset into focus */
class FocusAssetEvent(val url: String) : InternalEvent()

/** Internal event dispatched when the client was resumed */
class LifecycleResumedEvent : InternalEvent()

/** Internal event dispatched when the client was paused */
class LifecyclePausedEvent : InternalEvent()
