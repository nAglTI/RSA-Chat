package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User

interface CacheAware {
    val cache: Cache
}

class Cache {
    private var _ownUser: User? = null

    private val _guilds: MutableMap<Snowflake, Guild> = hashMapOf()

    private val _users: MutableMap<Snowflake, User> = hashMapOf()

    private val _channels: MutableMap<Snowflake, Channel> = hashMapOf()

    private val _messages: MutableMap<Snowflake, ArrayDeque<Message>> = hashMapOf()

    private val _members: MutableMap<Pair<Snowflake, Snowflake>, Member> = hashMapOf()


    val channels: Map<Snowflake, Channel> get() = _channels

    val guilds: Map<Snowflake, Guild> get() = _guilds

    val users: Map<Snowflake, User> get() = _users

    val members: Map<Pair<Snowflake, Snowflake>, Member> get() = _members

    val ownUser: User? get() = _ownUser

    val messages: Map<Snowflake, List<Message>> get() = _messages

    fun clear() {
        _guilds.clear()
        _users.clear()
        _channels.clear()
        _messages.clear()
        _members.clear()
        _ownUser = null
    }

    fun getGuild(guildId: Snowflake): Guild? {
        return _guilds[guildId]
    }

    fun getChannel(channelId: Snowflake): Channel? {
        return _channels[channelId]
    }

    fun getChannelsForGuild(guildId: Snowflake): Map<Snowflake, Channel> {
        return _channels.filterValues { it.guildId == guildId }
    }

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

    fun getUser(userId: Snowflake): User? {
        return _users[userId]
    }

    fun getMember(guildId: Snowflake, userId: Snowflake): Member? {
        return _members[Pair(guildId, userId)]
    }

    fun putGuild(guild: Guild) {
        _guilds[guild.id] = guild
    }

    fun putChannel(channel: Channel) {
        _channels[channel.id] = channel
    }

    fun putUser(user: User) {
        _users[user.id] = user
    }

    fun putMember(member: Member) {
        _members[Pair(member.guildId, member.id)] = member
    }

    fun putOwnUser(user: User) {
        _ownUser = user
    }

    fun addMessage(message: Message) {
        val messages = _messages[message.channelId] ?: ArrayDeque()
        messages.add(message)

        if (messages.size > 1000) {
            messages.removeFirst()
        }

        _messages[message.channelId] = messages
    }

    fun updateMessage(message: Message) {
        val messages = _messages[message.channelId] ?: return
        val index = messages.indexOfFirst { it.id == message.id }

        if (index != -1) {
            messages[index] = message
        }
    }

    fun dropMessage(channelId: Snowflake, messageId: Snowflake) {
        val messages = _messages[channelId] ?: return
        val index = messages.indexOfFirst { it.id == messageId }

        if (index != -1) {
            messages.removeAt(index)
        }
    }

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

    fun dropGuild(guildId: Snowflake) {
        val guildChannels = _channels.filterValues { it.guildId == guildId }.map { it.value.id }
        val guildMembers = _members.filterKeys { it.first == guildId }.map { it.value.id }

        guildChannels.forEach { dropChannel(it) }
        guildMembers.forEach { dropMember(guildId, it) }
        _guilds.remove(guildId)
    }

    fun dropChannel(channelId: Snowflake) {
        _channels.remove(channelId)
    }

    fun dropUser(userId: Snowflake) {
        _users.remove(userId)
    }

    fun dropMember(guildId: Snowflake, userId: Snowflake) {
        _members.remove(Pair(guildId, userId))
    }
}
