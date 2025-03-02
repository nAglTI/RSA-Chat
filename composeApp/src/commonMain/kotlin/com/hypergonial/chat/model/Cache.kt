package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import com.hypergonial.chat.subList
import kotlinx.datetime.Clock

/** A type that has a cache. */
interface CacheAware {
    val cache: Cache
}

data class MessageCache(
    val channelId: Snowflake,
    val messages: ArrayDeque<Message> = ArrayDeque(),
    var isCruising: Boolean = false,
)

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "This API is experimental and may change in the future.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DelicateCacheApi

/** A class that represents a cache of incoming gateway data. */
class Cache(private val cachedChannelsCount: Int = 10) {
    private var _ownUser: User? = null

    private val _guilds: MutableMap<Snowflake, Guild> = hashMapOf()

    private val _users: MutableMap<Snowflake, User> = hashMapOf()

    private val _channels: MutableMap<Snowflake, Channel> = hashMapOf()

    // This is a nested map of maps as opposed to a pair keyed map because
    // it is a common operation to query all typing indicators in a channel.
    private val _typingIndicators: MutableMap<Snowflake, HashMap<Snowflake, TypingIndicator>> = hashMapOf()

    private val _messages: MutableList<MessageCache> = mutableListOf()

    private val _members: MutableMap<Pair<Snowflake, Snowflake>, Member> = hashMapOf()

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
    val members: Map<Pair<Snowflake, Snowflake>, Member>
        get() = _members

    /** A map of all typing indicators in the cache, keyed by channel ID. */
    val typingIndicators: Map<Snowflake, Map<Snowflake, TypingIndicator>>
        get() = _typingIndicators

    /** The current user, if cached */
    val ownUser: User?
        get() = _ownUser

    /** Clear the cache. Drops all values. */
    fun clear() {
        _guilds.clear()
        _users.clear()
        _channels.clear()
        _messages.clear()
        _members.clear()
        _ownUser = null
    }

    /**
     * Returns the guild with the given ID, if it is cached.
     *
     * @param guildId The ID of the guild to get.
     * @return The guild with the given ID, if it is cached.
     */
    fun getGuild(guildId: Snowflake): Guild? {
        return _guilds[guildId]
    }

    /**
     * Returns the channel with the given ID, if it is cached.
     *
     * @param channelId The ID of the channel to get.
     * @return The channel with the given ID, if it is cached.
     */
    fun getChannel(channelId: Snowflake): Channel? {
        return _channels[channelId]
    }

    /**
     * Returns the channels for the given guild, if they are cached.
     *
     * @param guildId The ID of the guild to get channels for.
     * @return A map of channels for the given guild, keyed by channel ID.
     */
    fun getChannelsForGuild(guildId: Snowflake): Map<Snowflake, Channel> {
        return _channels.filterValues { it.guildId == guildId }
    }

    /**
     * Retain typing indicators that pass the given filter.
     *
     * @param f The filter to apply to the typing indicators. It is passed the channel ID and the typing indicator.
     */
    internal fun retainTypingIndicators(f: (Snowflake, TypingIndicator) -> Boolean) {
        for ((channelId, indicators) in _typingIndicators) {
            indicators.values.retainAll { f(channelId, it) }
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
        val indicators = _typingIndicators.getOrPut(channelId) { hashMapOf() }
        return indicators.put(userId, TypingIndicator(userId, Clock.System.now())) == null
    }

    /**
     * Remove a typing indicator.
     *
     * @param channelId The ID of the channel the typing indicator is in.
     * @param userId The ID of the user that is typing.
     * @return True if the typing indicator was removed, false if it was not present.
     */
    internal fun removeTypingIndicator(channelId: Snowflake, userId: Snowflake): Boolean {
        return _typingIndicators[channelId]?.remove(userId) != null
    }

    /**
     * Get typing indicators for a channel.
     *
     * @param channelId The ID of the channel to get typing indicators for.
     * @return A set of typing indicators for the given channel.
     */
    fun getTypingIndicators(channelId: Snowflake): Collection<TypingIndicator> {
        return _typingIndicators[channelId]?.values ?: emptySet()
    }

    /**
     * Get a typing indicator for a channel.
     *
     * @param channelId The ID of the channel the typing indicator is in.
     * @param userId The ID of the user that is typing.
     * @return The typing indicator for the given channel and user.
     */
    fun getTypingIndicator(channelId: Snowflake, userId: Snowflake): TypingIndicator? {
        return _typingIndicators[channelId]?.get(userId)
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
        require(listOfNotNull(before, after, around).size <= 1) { "Only one of before, after, and around can be set" }

        if (limit <= 0) return emptyList()

        val messages = _messages.getForChannel(channelId)?.messages ?: return emptyList()
        val anchorId =
            before
                ?: after
                ?: around
                ?: return messages.subList((messages.size - limit).coerceAtLeast(0)..<messages.size)

        // If item is not found, binarySearch returns the negative insertion point - 1
        // Don't question the magic offsets, they work
        val anchorOffset = if (before != null) -1 else -2
        val anchorIdx = messages.binarySearch { it.id.compareTo(anchorId) }.let { if (it < 0) -(it) + anchorOffset else it }

        val (start, end) =
            when {
                before != null -> Pair(anchorIdx - limit, anchorIdx)
                after != null -> Pair(anchorIdx + 1, anchorIdx + limit + 1)
                around != null -> {
                    val beforeCount = limit / 2
                    val afterCount = limit - beforeCount

                    Pair(anchorIdx - beforeCount, anchorIdx + afterCount)
                }
                else -> error("Invalid state")
            }

        return messages.subList(
            start.coerceAtLeast(0).coerceAtMost(messages.size),
            end.coerceAtLeast(0).coerceAtMost(messages.size),
        )
    }

    /**
     * Returns the user with the given ID, if it is cached.
     *
     * @param userId The ID of the user to get.
     * @return The user with the given ID, if it is cached.
     */
    fun getUser(userId: Snowflake): User? {
        return _users[userId]
    }

    /**
     * Returns the member with the given guild and user IDs, if it is cached.
     *
     * @param guildId The ID of the guild the member is in.
     * @param userId The ID of the member to get.
     * @return The member with the given guild and user IDs, if it is cached.
     */
    fun getMember(guildId: Snowflake, userId: Snowflake): Member? {
        return _members[Pair(guildId, userId)]
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
        return guildId?.let { getMember(it, userId) } ?: getUser(userId)
    }

    /**
     * Insert or update a guild in the cache.
     *
     * @param guild The guild to insert or update.
     */
    internal fun putGuild(guild: Guild) {
        _guilds[guild.id] = guild
    }

    /**
     * Insert or update a channel in the cache.
     *
     * @param channel The channel to insert or update.
     */
    internal fun putChannel(channel: Channel) {
        _channels[channel.id] = channel
    }

    /**
     * Insert or update a user in the cache.
     *
     * @param user The user to insert or update.
     */
    internal fun putUser(user: User) {
        _users[user.id] = user
    }

    /**
     * Insert or update a member in the cache.
     *
     * @param member The member to insert or update.
     */
    internal fun putMember(member: Member) {
        _members[Pair(member.guildId, member.id)] = member
    }

    /**
     * Insert or update the current user in the cache.
     *
     * @param user The current user to insert or update.
     */
    internal fun putOwnUser(user: User) {
        _ownUser = user
    }

    /**
     * Insert a message in the cache.
     *
     * @param message The message to insert or update. If no cache was registered for the given channel, or a previously
     *   registered cache was evicted, the message is dropped.
     */
    @DelicateCacheApi
    internal fun addMessage(message: Message) {
        val msgcache = _messages.getForChannel(message.channelId) ?: return

        if (msgcache.messages.lastOrNull()?.let { message.id > it.id } != false) {
            println("Higher than last")
            msgcache.messages.add(message)
        } else if (msgcache.messages.firstOrNull()?.let { message.id < it.id } != false) {
            println("Lower than first")

            msgcache.messages.addFirst(message)
        } else {
            error("Message cache is not sorted")
        }
    }

    /**
     * When called, it reserves a spot in the cache for messages in the given channel.
     *
     * It evicts the oldest channel's messages if the cache is full.
     *
     * @param channelId The ID of the channel to reserve a spot for.
     */
    internal fun registerMessageCacheFor(channelId: Snowflake) {
        _messages.getOrPutForChannel(channelId)
    }

    /**
     * Update a message in the cache.
     *
     * @param message The message to update.
     */
    internal fun updateMessage(message: Message) {
        val msgcache = _messages.getForChannel(message.channelId) ?: return
        val index = msgcache.messages.binarySearch { it.id.compareTo(message.id) }

        if (index >= 0) {
            msgcache.messages[index] = message
        }
    }

    /**
     * Drop a message from the cache.
     *
     * @param channelId The ID of the channel the message is in.
     * @param messageId The ID of the message to drop.
     */
    internal fun dropMessage(channelId: Snowflake, messageId: Snowflake) {
        val msgcache = _messages.getForChannel(channelId) ?: return
        val index = msgcache.messages.binarySearch { it.id.compareTo(messageId) }

        if (index >= 0) {
            msgcache.messages.removeAt(index)
        }
    }

    /**
     * Add a list of messages to the cache.
     *
     * @param messages The messages to add. The messages must all belong to the same channel and be sorted by ID. If the
     *   cache does not exist for the given channel, the messages are dropped.
     */
    @DelicateCacheApi
    internal fun addMessages(channelId: Snowflake, messages: List<Message>) {
        if (messages.isEmpty()) {
            return
        }

        val msgcache = _messages.getForChannel(channelId) ?: return

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
                error("Message cache is not sorted")
            }
        }
    }

    /**
     * Drop a guild from the cache. Also drops all channels, members, and messages associated with the guild.
     *
     * @param guildId The ID of the guild to drop.
     */
    internal fun dropGuild(guildId: Snowflake) {
        val guildChannels = _channels.filterValues { it.guildId == guildId }.map { it.value.id }
        val guildMembers = _members.filterKeys { it.first == guildId }.map { it.value.id }

        guildChannels.forEach { dropChannel(it) }
        guildMembers.forEach { dropMember(guildId, it) }
        _guilds.remove(guildId)
    }

    /**
     * Drop a channel from the cache. Also drops all messages associated with the channel.
     *
     * @param channelId The ID of the channel to drop.
     */
    internal fun dropChannel(channelId: Snowflake) {
        _channels.remove(channelId)
        _messages.dropForChannel(channelId)
    }

    /**
     * Drop a user from the cache.
     *
     * @param userId The ID of the user to drop.
     */
    internal fun dropUser(userId: Snowflake) {
        _users.remove(userId)
    }

    /**
     * Drop a member from the cache.
     *
     * @param guildId The ID of the guild the member is in.
     * @param userId The ID of the member to drop.
     */
    internal fun dropMember(guildId: Snowflake, userId: Snowflake) {
        _members.remove(Pair(guildId, userId))
    }

    /**
     * Retrieve the message cache for the given channel ID, if one exists.
     *
     * @param channelId The ID of the channel to get the message cache for.
     */
    private fun MutableList<MessageCache>.getForChannel(channelId: Snowflake): MessageCache? {
        return firstOrNull { it.channelId == channelId }
    }

    /**
     * Get the message cache for the given channel ID. If the cache does not exist, it is created.
     *
     * If the amount of message caches is higher than the limit, the oldest cache is removed.
     *
     * @param channelId The ID of the channel to get the message cache for.
     */
    private fun MutableList<MessageCache>.getOrPutForChannel(channelId: Snowflake): MessageCache {
        var idx = indexOfFirst { it.channelId == channelId }
        if (idx == -1) {
            if (size >= cachedChannelsCount) {
                removeFirst()
            }

            add(MessageCache(channelId))
            idx = size - 1
        }

        return this[idx]
    }

    /**
     * Check if the cache has a message cache for the given channel ID.
     *
     * @param channelId The ID of the channel to check for.
     */
    private fun MutableList<MessageCache>.hasChannelCached(channelId: Snowflake): Boolean {
        return indexOfFirst { it.channelId == channelId } != -1
    }

    private fun MutableList<MessageCache>.dropForChannel(channelId: Snowflake) {
        val idx = indexOfFirst { it.channelId == channelId }
        if (idx != -1) {
            removeAt(idx)
        }
    }
}
