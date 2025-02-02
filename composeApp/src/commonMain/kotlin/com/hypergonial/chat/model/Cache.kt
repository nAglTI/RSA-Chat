package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User

/** A type that has a cache. */
interface CacheAware {
    val cache: Cache
}

/** A class that represents a cache of incoming gateway data. */
class Cache {
    private var _ownUser: User? = null

    private val _guilds: MutableMap<Snowflake, Guild> = hashMapOf()

    private val _users: MutableMap<Snowflake, User> = hashMapOf()

    private val _channels: MutableMap<Snowflake, Channel> = hashMapOf()

    private val _messages: MutableMap<Snowflake, ArrayDeque<Message>> = hashMapOf()

    private val _members: MutableMap<Pair<Snowflake, Snowflake>, Member> = hashMapOf()


    /** A map of all channels in the cache, keyed by channel ID. */
    val channels: Map<Snowflake, Channel> get() = _channels

    /** A map of all guilds in the cache, keyed by guild ID. */
    val guilds: Map<Snowflake, Guild> get() = _guilds

    /** A map of all users in the cache, keyed by user ID. */
    val users: Map<Snowflake, User> get() = _users

    /** A map of all members in the cache, keyed by a pair of guild ID and user ID. */
    val members: Map<Pair<Snowflake, Snowflake>, Member> get() = _members

    /** The current user, if cached */
    val ownUser: User? get() = _ownUser

    /** A map of all messages in the cache, keyed by channel ID. */
    val messages: Map<Snowflake, List<Message>> get() = _messages

    /** Clear the cache. Drops all values. */
    fun clear() {
        _guilds.clear()
        _users.clear()
        _channels.clear()
        _messages.clear()
        _members.clear()
        _ownUser = null
    }

    /** Returns the guild with the given ID, if it is cached.
     *
     * @param guildId The ID of the guild to get.
     *
     * @return The guild with the given ID, if it is cached.
     * */
    fun getGuild(guildId: Snowflake): Guild? {
        return _guilds[guildId]
    }

    /** Returns the channel with the given ID, if it is cached.
     *
     * @param channelId The ID of the channel to get.
     *
     * @return The channel with the given ID, if it is cached.
     * */
    fun getChannel(channelId: Snowflake): Channel? {
        return _channels[channelId]
    }

    /** Returns the channels for the given guild, if they are cached.
     *
     * @param guildId The ID of the guild to get channels for.
     *
     * @return A map of channels for the given guild, keyed by channel ID.
     * */
    fun getChannelsForGuild(guildId: Snowflake): Map<Snowflake, Channel> {
        return _channels.filterValues { it.guildId == guildId }
    }

    /** Returns messages for the given channel, if they are cached.
     *
     * Note that only one of before, after, and around can be set.
     *
     * @param channelId The ID of the channel to get messages for.
     * @param before The ID of the message to get messages before.
     * @param after The ID of the message to get messages after.
     * @param around The ID of the message to get messages around.
     * @param limit The maximum number of messages to return.
     *
     * @return A list of messages matching the given criteria.
     * */
    fun getMessagesWhere(
        channelId: Snowflake,
        before: Snowflake? = null,
        after: Snowflake? = null,
        around: Snowflake? = null,
        limit: Int = 100
    ): List<Message> {
        require(listOfNotNull(before, after, around).size <= 1) {
            "Only one of before, after, and around can be set"
        }

        val messages = _messages[channelId] ?: return emptyList()

        val start = when {
            before != null -> messages.indexOfFirst { it.id == before } + 1
            after != null -> messages.indexOfFirst { it.id == after } - 1
            around != null -> messages.indexOfFirst { it.id == around } - (limit / 2)
            else -> 0
        }

        val end = when {
            before != null -> start + limit
            after != null -> start - limit
            around != null -> start + (limit / 2)
            else -> start + limit
        }

        return messages.subList(
            start.coerceAtLeast(0).coerceAtMost(messages.size),
            end.coerceAtLeast(0).coerceAtMost(messages.size)
        )
    }


    /** Returns the user with the given ID, if it is cached.
     *
     * @param userId The ID of the user to get.
     *
     * @return The user with the given ID, if it is cached.
     * */
    fun getUser(userId: Snowflake): User? {
        return _users[userId]
    }

    /** Returns the member with the given guild and user IDs, if it is cached.
     *
     * @param guildId The ID of the guild the member is in.
     * @param userId The ID of the member to get.
     *
     * @return The member with the given guild and user IDs, if it is cached.
     * */
    fun getMember(guildId: Snowflake, userId: Snowflake): Member? {
        return _members[Pair(guildId, userId)]
    }

    /** Insert or update a guild in the cache.
     *
     * @param guild The guild to insert or update.
     * */
    fun putGuild(guild: Guild) {
        _guilds[guild.id] = guild
    }

    /** Insert or update a channel in the cache.
     *
     * @param channel The channel to insert or update.
     * */
    fun putChannel(channel: Channel) {
        _channels[channel.id] = channel
    }

    /** Insert or update a user in the cache.
     *
     * @param user The user to insert or update.
     * */
    fun putUser(user: User) {
        _users[user.id] = user
    }

    /** Insert or update a member in the cache.
     *
     * @param member The member to insert or update.
     * */
    fun putMember(member: Member) {
        _members[Pair(member.guildId, member.id)] = member
    }

    /** Insert or update the current user in the cache.
     *
     * @param user The current user to insert or update.
     * */
    fun putOwnUser(user: User) {
        _ownUser = user
    }

    /** Insert a message in the cache.
     *
     * @param message The message to insert or update.
     * */
    fun addMessage(message: Message) {
        val messages = _messages[message.channelId] ?: ArrayDeque()
        messages.add(message)

        if (messages.size > 1000) {
            messages.removeFirst()
        }

        _messages[message.channelId] = messages
    }

    /** Update a message in the cache.
     *
     * @param message The message to update.
     * */
    fun updateMessage(message: Message) {
        val messages = _messages[message.channelId] ?: return
        val index = messages.indexOfFirst { it.id == message.id }

        if (index != -1) {
            messages[index] = message
        }
    }

    /** Drop a message from the cache.
     *
     * @param channelId The ID of the channel the message is in.
     * @param messageId The ID of the message to drop.
     * */
    fun dropMessage(channelId: Snowflake, messageId: Snowflake) {
        val messages = _messages[channelId] ?: return
        val index = messages.indexOfFirst { it.id == messageId }

        if (index != -1) {
            messages.removeAt(index)
        }
    }

    /** Add a list of messages to the cache.
     *
     * @param messages The messages to add.
     * */
    fun addMessages(messages: List<Message>) {
        val grouped = messages.groupBy { it.channelId }

        for ((channelId, group) in grouped) {
            val queue = _messages[channelId] ?: ArrayDeque()
            queue.addAll(group)

            if (queue.size > 1000) {
                queue.subList(0, queue.size - 1000).clear()
            }

            _messages[channelId] = queue
        }
    }

    /** Drop a guild from the cache. Also drops all channels, members, and messages associated with the guild.
     *
     * @param guildId The ID of the guild to drop.
     * */
    fun dropGuild(guildId: Snowflake) {
        val guildChannels = _channels.filterValues { it.guildId == guildId }.map { it.value.id }
        val guildMembers = _members.filterKeys { it.first == guildId }.map { it.value.id }

        guildChannels.forEach { dropChannel(it) }
        guildMembers.forEach { dropMember(guildId, it) }
        _guilds.remove(guildId)
    }

    /** Drop a channel from the cache. Also drops all messages associated with the channel.
     *
     * @param channelId The ID of the channel to drop.
     * */
    fun dropChannel(channelId: Snowflake) {
        _channels.remove(channelId)
        _messages.remove(channelId)
    }

    /** Drop a user from the cache.
     *
     * @param userId The ID of the user to drop.
     * */
    fun dropUser(userId: Snowflake) {
        _users.remove(userId)
    }

    /** Drop a member from the cache.
     *
     * @param guildId The ID of the guild the member is in.
     * @param userId The ID of the member to drop.
     * */
    fun dropMember(guildId: Snowflake, userId: Snowflake) {
        _members.remove(Pair(guildId, userId))
    }
}
