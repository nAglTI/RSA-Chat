package com.hypergonial.chat.model

import co.touchlab.kermit.Logger
import com.hypergonial.chat.levenshteinDistance
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.datetime.Clock

/** A type that has a cache. */
interface CacheAware {
    val cache: Cache
}

data class MessageCache(
    /** The ID of the channel this cache is for. */
    val channelId: Snowflake,
    /** The messages in the cache, ordered by ID. */
    val messages: ArrayDeque<Message> = ArrayDeque(),
    /** If true, it means the beginning of the message list is no longer in cache */
    var isCruising: Boolean = false,
    /** If true, it means the end of the message list is cached */
    var hasEnd: Boolean = false,
) {
    fun reset() {
        messages.clear()
        isCruising = false
        hasEnd = false
    }
}

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API requires extra attention before use. Read the documentation.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DelicateCacheApi

/** A class that represents a cache of incoming gateway data. */
class Cache(private val cachedChannelsCount: Int = 10) : SynchronizedObject() {
    private var _ownUser: User? = null

    private val _guilds: MutableMap<Snowflake, Guild> = hashMapOf()

    private val _users: MutableMap<Snowflake, User> = hashMapOf()

    private val _channels: MutableMap<Snowflake, Channel> = hashMapOf()

    // This is a nested map of maps as opposed to a pair keyed map because
    // it is a common operation to query all typing indicators in a channel.
    private val _typingIndicators: MutableMap<Snowflake, HashMap<Snowflake, TypingIndicator>> = hashMapOf()

    private val _msgcaches: MutableList<MessageCache> = mutableListOf()

    private val _readStates: MutableMap<Snowflake, ReadState> = hashMapOf()

    private val _members: MutableMap<Snowflake, MutableMap<Snowflake, Member>> = hashMapOf()

    private val logger = Logger.withTag("Cache")

    /** A map of all channels in the cache, keyed by channel ID. */
    val channels: Map<Snowflake, Channel>
        get() = _channels

    /** A map of all guilds in the cache, keyed by guild ID. */
    val guilds: Map<Snowflake, Guild>
        get() = _guilds

    /** A map of all users in the cache, keyed by user ID. */
    val users: Map<Snowflake, User>
        get() = _users

    /** A map of all members in the cache, keyed by a pair of guild ID and user ID. */
    val members: Map<Snowflake, MutableMap<Snowflake, Member>>
        get() = _members

    /** A map of all typing indicators in the cache, keyed by channel ID. */
    val typingIndicators: Map<Snowflake, Map<Snowflake, TypingIndicator>>
        get() = _typingIndicators

    /** The current user, if cached */
    val ownUser: User?
        get() = _ownUser

    /** Clear the cache. Drops all values. */
    fun clear() {
        synchronized(this) {
            _guilds.clear()
            _users.clear()
            _channels.clear()
            _msgcaches.clear()
            _members.clear()
            _typingIndicators.clear()
            _readStates.clear()
            _msgcaches.forEach { it.reset() }
            _ownUser = null
        }
    }

    fun clearMessageCache() {
        // It's important to not delete the cache objects as that will
        // invalidate registrations and potentially break app logic
        // Caches will be pushed out as new ones come in anyway
        synchronized(this) { _msgcaches.forEach { it.reset() } }
    }

    /**
     * Returns the guild with the given ID, if it is cached.
     *
     * @param guildId The ID of the guild to get.
     * @return The guild with the given ID, if it is cached.
     */
    fun getGuild(guildId: Snowflake): Guild? {
        synchronized(this) {
            return _guilds[guildId]
        }
    }

    /**
     * Returns the channel with the given ID, if it is cached.
     *
     * @param channelId The ID of the channel to get.
     * @return The channel with the given ID, if it is cached.
     */
    fun getChannel(channelId: Snowflake): Channel? {
        synchronized(this) {
            return _channels[channelId]
        }
    }

    /**
     * Returns the channels for the given guild, if they are cached.
     *
     * @param guildId The ID of the guild to get channels for.
     * @return A map of channels for the given guild, keyed by channel ID.
     */
    fun getChannelsForGuild(guildId: Snowflake): Map<Snowflake, Channel> {
        synchronized(this) {
            return _channels.filterValues { it.guildId == guildId }
        }
    }

    /**
     * Retain typing indicators that pass the given filter.
     *
     * @param f The filter to apply to the typing indicators. It is passed the channel ID and the typing indicator.
     */
    internal fun retainTypingIndicators(f: (Snowflake, TypingIndicator) -> Boolean) {
        synchronized(this) {
            for ((channelId, indicators) in _typingIndicators) {
                indicators.values.retainAll { f(channelId, it) }
            }
        }
    }

    /**
     * Add or update a typing indicator.
     *
     * @param channelId The ID of the channel the typing indicator is in.
     * @param userId The ID of the user that is typing.
     * @return True if a new typing indicator was added, false if it was updated.
     */
    internal fun updateTypingIndicator(channelId: Snowflake, userId: Snowflake): Boolean {
        synchronized(this) {
            val indicators = _typingIndicators.getOrPut(channelId) { hashMapOf() }
            return indicators.put(userId, TypingIndicator(userId, Clock.System.now())) == null
        }
    }

    /**
     * Remove a typing indicator.
     *
     * @param channelId The ID of the channel the typing indicator is in.
     * @param userId The ID of the user that is typing.
     * @return True if the typing indicator was removed, false if it was not present.
     */
    internal fun removeTypingIndicator(channelId: Snowflake, userId: Snowflake): Boolean {
        synchronized(this) {
            return _typingIndicators[channelId]?.remove(userId) != null
        }
    }

    /**
     * Get typing indicators for a channel.
     *
     * @param channelId The ID of the channel to get typing indicators for.
     * @return A set of typing indicators for the given channel.
     */
    fun getTypingIndicators(channelId: Snowflake): Collection<TypingIndicator> {
        synchronized(this) {
            return _typingIndicators[channelId]?.values ?: emptySet()
        }
    }

    /**
     * Get a typing indicator for a channel.
     *
     * @param channelId The ID of the channel the typing indicator is in.
     * @param userId The ID of the user that is typing.
     * @return The typing indicator for the given channel and user.
     */
    fun getTypingIndicator(channelId: Snowflake, userId: Snowflake): TypingIndicator? {
        synchronized(this) {
            return _typingIndicators[channelId]?.get(userId)
        }
    }

    /**
     * Returns messages for the given channel, if they are cached.
     *
     * Note that only one of before, after, and around can be set.
     *
     * @param channelId The ID of the channel to get messages for.
     * @param before The ID of the message to get messages before.
     * @param after The ID of the message to get messages after.
     * @param around The ID of the message to get messages around.
     * @param limit The maximum number of messages to return.
     * @return A list of messages matching the given criteria.
     */
    fun getMessages(
        channelId: Snowflake,
        before: Snowflake? = null,
        after: Snowflake? = null,
        around: Snowflake? = null,
        limit: Int = 100,
    ): List<Message> {
        synchronized(this) {
            require(listOfNotNull(before, after, around).size <= 1) {
                "Only one of before, after, and around can be set"
            }

            if (limit <= 0) return emptyList()

            val messages = _msgcaches.getForChannel(channelId)?.messages ?: return emptyList()
            val anchorId =
                before
                    ?: after
                    ?: around
                    ?: return messages.slice((messages.size - limit).coerceAtLeast(0) until messages.size)

            // If item is not found, binarySearch returns the negative insertion point - 1
            // Don't question the magic offsets, they work
            val anchorOffset = if (before != null) -1 else -2
            val anchorIdx =
                messages.binarySearch { it.id.compareTo(anchorId) }.let { if (it < 0) -(it) + anchorOffset else it }

            val (start, end) =
                when {
                    before != null -> (anchorIdx - limit) to anchorIdx
                    after != null -> (anchorIdx + 1) to (anchorIdx + limit + 1)
                    around != null -> {
                        val beforeCount = limit / 2
                        val afterCount = limit - beforeCount

                        (anchorIdx - beforeCount) to (anchorIdx + afterCount)
                    }
                    else -> error("Invalid state")
                }

            return messages.slice(
                start.coerceAtLeast(0).coerceAtMost(messages.size) until
                    end.coerceAtLeast(0).coerceAtMost(messages.size)
            )
        }
    }

    /**
     * If true, the cache holds the beginning of the message list for the given channel.
     *
     * This means that fetches before the first message in the cache will always return an empty list, meaning there is
     * no point in fetching more messages before the first message in the cache.
     */
    fun hasEndCached(channelId: Snowflake): Boolean {
        synchronized(this) {
            return _msgcaches.getForChannel(channelId)?.hasEnd == true
        }
    }

    /**
     * Returns the user with the given ID, if it is cached.
     *
     * @param userId The ID of the user to get.
     * @return The user with the given ID, if it is cached.
     */
    fun getUser(userId: Snowflake): User? {
        synchronized(this) {
            return _users[userId]
        }
    }

    /**
     * Returns the member with the given guild and user IDs, if it is cached.
     *
     * @param guildId The ID of the guild the member is in.
     * @param userId The ID of the member to get.
     * @return The member with the given guild and user IDs, if it is cached.
     */
    fun getMember(guildId: Snowflake, userId: Snowflake): Member? {
        synchronized(this) {
            return _members[guildId]?.get(userId)
        }
    }

    /**
     * Returns all members in the cache for the given guild.
     *
     * @param guildId The ID of the guild to get members for.
     * @return A list of members in the given guild.
     */
    fun getGuildMembers(guildId: Snowflake): List<Member> {
        synchronized(this) {
            return _members[guildId]?.values?.toList() ?: emptyList()
        }
    }

    /**
     * Returns members that match the input string the closest.
     *
     * @param guildId The ID of the guild to get members for.
     * @param input The input string to match against.
     * @param count The maximum number of members to return.
     * @return A list of members in the given guild that match the input string the closest.
     */
    fun getClosestMemberMatches(guildId: Snowflake, input: String, count: Int = 10): List<Member> {
        val members = getGuildMembers(guildId)

        if (input == "") {
            // Always try to include ourselves in the list first
            val ownMember = getMember(guildId, ownUser?.id ?: return members.take(count)) ?: return members.take(count)

            return listOf(ownMember) + members.take(count - 1)
        }

        return members
            .sortedBy { member ->
                // Calculate minimum distance among all possible names
                minOf(
                    member.username.levenshteinDistance(input),
                    member.displayName?.levenshteinDistance(input) ?: Int.MAX_VALUE,
                    member.nickname?.levenshteinDistance(input) ?: Int.MAX_VALUE,
                )
            }
            .take(count)
    }

    /**
     * Returns the member with the given guild and user IDs, if it is cached. If no guild ID is provided, returns the
     * user with the given ID, if it is cached.
     *
     * @param guildId The ID of the guild the member is in.
     * @param userId The ID of the member to get.
     * @return The member or user with the given guild and user IDs, if it is cached. Implementors should check the
     *   return value's type to determine if it is a member or user.
     */
    fun getMemberOrUser(userId: Snowflake, guildId: Snowflake? = null): User? {
        synchronized(this) {
            return guildId?.let { getMember(it, userId) } ?: getUser(userId)
        }
    }

    /**
     * Insert or update a guild in the cache.
     *
     * @param guild The guild to insert or update.
     */
    internal fun putGuild(guild: Guild) = synchronized(this) { _guilds[guild.id] = guild }

    /**
     * Insert or update a channel in the cache.
     *
     * @param channel The channel to insert or update.
     */
    internal fun putChannel(channel: Channel) = synchronized(this) { _channels[channel.id] = channel }

    /**
     * Insert or update a user in the cache.
     *
     * @param user The user to insert or update.
     */
    internal fun putUser(user: User) = synchronized(this) { _users[user.id] = user }

    /**
     * Insert or update a member in the cache.
     *
     * @param member The member to insert or update.
     */
    internal fun putMember(member: Member) =
        synchronized(this) { _members.getOrPut(member.guildId) { HashMap() }[member.id] = member }

    /**
     * Insert or update the current user in the cache.
     *
     * @param user The current user to insert or update.
     */
    internal fun putOwnUser(user: User) = synchronized(this) { _ownUser = user }

    /**
     * Insert a message in the cache.
     *
     * One should call [registerMessageCacheFor] at some point before calling this method.
     *
     * @param message The message to insert or update. If no cache was registered for the given channel, or a previously
     *   registered cache was evicted, this does nothing.
     */
    @DelicateCacheApi
    internal fun addMessage(message: Message) {
        synchronized(this) {
            val msgcache = _msgcaches.getForChannel(message.channelId) ?: return@synchronized

            if (msgcache.messages.lastOrNull()?.let { message.id > it.id } != false) {
                msgcache.messages.add(message)
            } else if (msgcache.messages.firstOrNull()?.let { message.id < it.id } != false) {

                msgcache.messages.addFirst(message)
            } else {
                logger.w {
                    "Message cache is not sorted or invalid input was passed during single insertion, " +
                        "this may cause performance problems as the cache needs to be re-sorted. (This is a bug)"
                }
                msgcache.messages.add(message)
                msgcache.messages.sortBy { it.id }
            }
        }
    }

    /**
     * Add a list of messages to the cache.
     *
     * One should call [registerMessageCacheFor] at some point before calling this method.
     *
     * @param messages The messages to add. The messages must all belong to the same channel and be sorted by ID. If no
     *   cache was registered for the given channel, or a previously registered cache was evicted, this does nothing.
     */
    @DelicateCacheApi
    internal fun addMessages(channelId: Snowflake, messages: List<Message>, hasEnd: Boolean = false) {
        synchronized(this) {
            val msgcache = _msgcaches.getForChannel(channelId) ?: return

            if (messages.isEmpty()) {
                msgcache.hasEnd = hasEnd
                return
            }

            if (msgcache.messages.isEmpty()) {
                msgcache.messages.addAll(messages)
            } else {
                val first = messages.first()
                val last = messages.last()

                if (msgcache.messages.last().id < first.id) {
                    msgcache.messages.addAll(messages)
                } else if (msgcache.messages.first().id > last.id) {
                    msgcache.messages.addAll(0, messages)
                } else {
                    logger.w {
                        "Message cache is not sorted or invalid input was passed," +
                            "this may cause performance problems as the cache needs to be re-sorted. (This is a bug)"
                    }
                    msgcache.messages.addAll(messages)
                    msgcache.messages.sortBy { it.id }
                }
            }
            msgcache.hasEnd = hasEnd
        }
    }

    /**
     * Update a message in the cache. If the message is not present, or if no cache was registered for the given
     * channel, or a previously registered cache was evicted, this does nothing.
     *
     * One should call [registerMessageCacheFor] at some point before calling this method.
     *
     * @param message The message to update.
     */
    @DelicateCacheApi
    internal fun updateMessage(message: Message) {
        synchronized(this) {
            val msgcache = _msgcaches.getForChannel(message.channelId) ?: return
            val index = msgcache.messages.binarySearch { it.id.compareTo(message.id) }

            if (index >= 0) {
                msgcache.messages[index] = message
            }
        }
    }

    /**
     * Drop a message from the cache. If the message is not present, does nothing.
     *
     * @param channelId The ID of the channel the message is in.
     * @param messageId The ID of the message to drop.
     */
    internal fun dropMessage(channelId: Snowflake, messageId: Snowflake) {
        synchronized(this) {
            val msgcache = _msgcaches.getForChannel(channelId) ?: return
            val index = msgcache.messages.binarySearch { it.id.compareTo(messageId) }

            if (index >= 0) {
                msgcache.messages.removeAt(index)
            }
        }
    }

    /**
     * Set the last message ID for a channel.
     *
     * This can be used for read state tracking.
     *
     * @param channelId The ID of the channel to set the last message ID for.
     * @param messageId The ID of the last message in the channel.
     */
    internal fun setLastMessageId(channelId: Snowflake, messageId: Snowflake?) {
        synchronized(this) {
            _readStates[channelId] =
                _readStates[channelId]?.copy(lastMessageId = messageId) ?: ReadState(messageId, null)
        }
    }

    /**
     * Set the last read message ID for a channel.
     *
     * This can be used for read state tracking.
     *
     * @param channelId The ID of the channel to set the last read message ID for.
     * @param messageId The ID of the last read message in the channel.
     */
    internal fun setLastReadMessageId(channelId: Snowflake, messageId: Snowflake?) {
        synchronized(this) {
            _readStates[channelId] =
                _readStates[channelId]?.copy(lastReadMessageId = messageId) ?: ReadState(null, messageId)
        }
    }

    internal fun setReadState(channelId: Snowflake, readState: ReadState) {
        synchronized(this) { _readStates[channelId] = readState }
    }

    /**
     * If true, the channel has un-acked messages.
     *
     * @param channelId The ID of the channel to check.
     * @return True if the channel has un-acked messages, false otherwise.
     */
    fun isUnread(channelId: Snowflake): Boolean {
        synchronized(this) {
            val readState = _readStates[channelId] ?: return false
            val lastReadTime =
                if (readState.lastReadMessageId != null) readState.lastReadMessageId.createdAt
                else {
                    val guild = _channels[channelId]?.guildId ?: return true
                    val member = _members[guild]?.get(ownUser!!.id) ?: return true
                    member.joinedAt
                }
            val lastMessageTime = readState.lastMessageId?.createdAt ?: return false

            return lastMessageTime > lastReadTime
        }
    }

    /**
     * If true, the guild has un-acked messages.
     *
     * @param guildId The ID of the guild to check.
     * @return True if the guild has un-acked messages, false otherwise.
     */
    fun isGuildUnread(guildId: Snowflake): Boolean {
        synchronized(this) {
            val channels = _channels.filterValues { it.guildId == guildId }.keys
            return channels.any { isUnread(it) }
        }
    }

    /**
     * Get the read state for a channel.
     *
     * @param channelId The ID of the channel to get the last message ID for.
     * @return The ID of the last message in the channel, if one is present.
     */
    fun getReadState(channelId: Snowflake): ReadState? =
        synchronized(this) {
            return _readStates[channelId]
        }

    /**
     * When called, it reserves a spot in the cache for messages in the given channel.
     *
     * It evicts the oldest channel's messages if the cache is full.
     *
     * @param channelId The ID of the channel to reserve a spot for.
     */
    internal fun registerMessageCacheFor(channelId: Snowflake) =
        synchronized(this) { _msgcaches.getOrPutForChannel(channelId) }

    /**
     * Drop a guild from the cache. Also drops all channels, members, and messages associated with the guild.
     *
     * @param guildId The ID of the guild to drop.
     */
    internal fun dropGuild(guildId: Snowflake) {
        synchronized(this) {
            val guildChannels = _channels.filterValues { it.guildId == guildId }.map { it.value.id }

            guildChannels.forEach { dropChannel(it) }
            _members.remove(guildId)
            _guilds.remove(guildId)
        }
    }

    /**
     * Drop a channel from the cache. Also drops all messages associated with the channel.
     *
     * @param channelId The ID of the channel to drop.
     */
    internal fun dropChannel(channelId: Snowflake) {
        synchronized(this) {
            _channels.remove(channelId)
            _msgcaches.dropForChannel(channelId)
        }
    }

    /**
     * Drop a user from the cache.
     *
     * @param userId The ID of the user to drop.
     */
    internal fun dropUser(userId: Snowflake) = synchronized(this) { _users.remove(userId) }

    /**
     * Drop a member from the cache.
     *
     * @param guildId The ID of the guild the member is in.
     * @param userId The ID of the member to drop.
     */
    internal fun dropMember(guildId: Snowflake, userId: Snowflake) {
        synchronized(this) { _members[guildId]?.remove(userId) }
    }

    /**
     * Retrieve the message cache for the given channel ID, if one exists.
     *
     * @param channelId The ID of the channel to get the message cache for.
     */
    private fun MutableList<MessageCache>.getForChannel(channelId: Snowflake): MessageCache? {
        synchronized(this@Cache) {
            return firstOrNull { it.channelId == channelId }
        }
    }

    /**
     * Get the message cache for the given channel ID. If the cache does not exist, it is created.
     *
     * If the amount of message caches is higher than the limit, the oldest cache is removed.
     *
     * @param channelId The ID of the channel to get the message cache for.
     */
    private fun MutableList<MessageCache>.getOrPutForChannel(channelId: Snowflake): MessageCache {
        synchronized(this@Cache) {
            var idx = indexOfFirst { it.channelId == channelId }
            if (idx == -1) {
                if (size >= cachedChannelsCount) {
                    removeAt(0)
                }

                add(MessageCache(channelId))
                idx = size - 1
            }

            return this[idx]
        }
    }

    private fun MutableList<MessageCache>.dropForChannel(channelId: Snowflake) {
        synchronized(this@Cache) {
            val idx = indexOfFirst { it.channelId == channelId }
            if (idx != -1) {
                removeAt(idx)
            }
        }
    }
}
