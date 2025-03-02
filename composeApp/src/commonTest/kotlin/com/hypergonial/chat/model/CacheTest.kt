package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(DelicateCacheApi::class)
class CacheTest {

    // Create a test message with given ID
    private fun createMessage(id: Int) = createMessage(Snowflake(id))

    private fun createMessage(id: Snowflake, channelId: Snowflake = Snowflake(1)): Message {
        return Message(
            id = id,
            channelId = channelId,
            author = User(id = Snowflake(100), username = "among-us"),
            content = "Test message ${id.inner}",
            isEdited = false,
            attachments = emptyList(),
        )
    }

    // Create a series of messages with consecutive IDs
    private fun createConsecutiveMessages(start: Int, count: Int): List<Message> {
        return (start until start + count).map { createMessage(it) }
    }

    private fun createConsecutiveMessages(start: Int, count: Int, step: Int): List<Message> {
        return (start until start + (count * step) step step).map { createMessage(it) }
    }

    @Test
    fun testCacheInitialization() {
        val cache = Cache()
        assertTrue(cache.channels.isEmpty())
        assertTrue(cache.guilds.isEmpty())
        assertTrue(cache.users.isEmpty())
        assertTrue(cache.members.isEmpty())
        assertTrue(cache.typingIndicators.isEmpty())
    }



    @Test
    fun testAddingSingleMessages() {
        val cache = Cache()

        // Register cache first
        cache.registerMessageCacheFor(Snowflake(1))

        cache.addMessage(createMessage(3))
        cache.addMessage(createMessage(4))
        cache.addMessage(createMessage(5))

        // Messages should be sorted by ID in ascending order
        val messages = cache.getMessages(Snowflake(1), limit = 10)
        assertEquals(3, messages.size)
        assertEquals(Snowflake(3), messages[0].id)
        assertEquals(Snowflake(4), messages[1].id)
        assertEquals(Snowflake(5), messages[2].id)
    }

    @Test
    fun testAddingNewFailsIfNoCache() {
        val cache = Cache()

        // New messages should not be added if there is no cache registered
        cache.addMessage(createMessage(3))
        cache.addMessage(createMessage(4))
        cache.addMessage(createMessage(5))

        val messages = cache.getMessages(Snowflake(1), limit = 10)
        assertEquals(0, messages.size)
    }

    @Test
    fun testAddingBatchToBack() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        // Add a batch of older messages
        val olderMessages = createConsecutiveMessages(1, 10)
        cache.addMessages(Snowflake(1), olderMessages)

        // Add a batch of newer messages
        val newerMessages = createConsecutiveMessages(11, 10)
        cache.addMessages(Snowflake(1), newerMessages)

        // Messages should be properly sorted
        val allMessages = cache.getMessages(Snowflake(1), limit = 100)
        assertEquals(20, allMessages.size)
        assertEquals(Snowflake(1), allMessages.first().id)
        assertEquals(Snowflake(20), allMessages.last().id)
    }

    @Test
    fun testAddingBatchToFront() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        // Add a batch of newer messages
        val olderMessages = createConsecutiveMessages(11, 10)
        cache.addMessages(Snowflake(1), olderMessages)

        // Add a batch of older messages
        val newerMessages = createConsecutiveMessages(1, 10)
        cache.addMessages(Snowflake(1), newerMessages)

        // Messages should be properly sorted
        val allMessages = cache.getMessages(Snowflake(1), limit = 100)
        assertEquals(20, allMessages.size)
        assertEquals(Snowflake(1), allMessages.first().id)
        assertEquals(Snowflake(20), allMessages.last().id)
    }

    @Test
    fun testChannelCacheLimit() {
        val channelLimit = 3
        val cache = Cache(cachedChannelsCount = channelLimit)

        // Create and add channels and messages
        for (i in 1..channelLimit + 2) {
            val channelSnowflake = Snowflake(i)
            cache.registerMessageCacheFor(channelSnowflake)

            // Add a message to each channel
            cache.addMessage(
                createMessage(i).copy(channelId = channelSnowflake),
            )
        }

        // We should only have messages cached for the most recent channels
        val channelsWithMessages =
            (1..channelLimit + 2)
                .map { Snowflake(it.toULong()) }
                .filter { cache.getMessages(it, limit = 1).isNotEmpty() }

        assertEquals(channelLimit, channelsWithMessages.size, "Should only keep messages for the most recent channels")

        // The most recent channels should have their messages cached
        for (i in channelLimit..channelLimit + 2) {
            val hasMessages = cache.getMessages(Snowflake(i), limit = 1).isNotEmpty()
            if (i <= channelLimit + 2 && i > 2) {
                assertTrue(hasMessages, "Channel $i should have cached messages")
            } else {
                assertFalse(hasMessages, "Channel $i should not have cached messages")
            }
        }
    }

    @Test
    fun testQueryWithBefore() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        // Add 20 messages
        val messages = createConsecutiveMessages(1, 20)
        cache.addMessages(Snowflake(1), messages)

        // Query before message 15, limit 5
        val beforeResults = cache.getMessages(Snowflake(1), before = Snowflake(15), limit = 5)

        assertEquals(5, beforeResults.size)
        assertEquals(Snowflake(10), beforeResults[0].id) // Should get 10-14
        assertEquals(Snowflake(14), beforeResults[4].id)
    }

    @Test
    fun testQueryWithAfter() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        // Add 20 messages
        val messages = createConsecutiveMessages(1, 20)
        cache.addMessages(Snowflake(1), messages)

        // Query after message 5, limit 3
        val afterResults = cache.getMessages(Snowflake(1), after = Snowflake(5), limit = 3)

        assertEquals(3, afterResults.size)
        assertEquals(Snowflake(6), afterResults[0].id) // Should get 6-8
        assertEquals(Snowflake(8), afterResults[2].id)
    }

    @Test
    fun testQueryWithAround() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        // Add 20 messages
        val messages = createConsecutiveMessages(1, 20)
        cache.addMessages(Snowflake(1), messages)

        // Query around message 10, limit 5 (should get 8-12)
        val aroundResults = cache.getMessages(Snowflake(1), around = Snowflake(10), limit = 5)

        assertEquals(5, aroundResults.size)
        assertEquals(Snowflake(8), aroundResults[0].id)
        assertEquals(Snowflake(10), aroundResults[2].id) // Middle message
        assertEquals(Snowflake(12), aroundResults[4].id)
    }

    @Test
    fun testMessageUpdateAndDelete() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        // Add messages
        for (i in 1..5) {
            cache.addMessage(createMessage(i))
        }

        // Update a message
        val updatedMessage = createMessage(3).copy(content = "Updated content")
        cache.updateMessage(updatedMessage)

        // Check if updated
        val retrievedAfterUpdate = cache.getMessages(Snowflake(1), limit = 10).first { it.id == Snowflake(3) }
        assertEquals("Updated content", retrievedAfterUpdate.content)

        // Delete a message
        cache.dropMessage(Snowflake(1), Snowflake(2))

        // Check if deleted
        val messagesAfterDelete = cache.getMessages(Snowflake(1), limit = 10)
        assertEquals(4, messagesAfterDelete.size)
        assertEquals(listOf(1uL, 3uL, 4uL, 5uL), messagesAfterDelete.map { it.id.inner })
    }

    @Test
    fun testQueryingWithNonExistent() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        cache.addMessages(Snowflake(1), createConsecutiveMessages(1, 10))
        cache.addMessages(Snowflake(1), createConsecutiveMessages(12, 10))

        // Query before non-existent message
        val beforeNonExistent = cache.getMessages(Snowflake(1), before = Snowflake(11), limit = 5)
        assertEquals(5, beforeNonExistent.size) // Should still return messages
        assertEquals(Snowflake(6), beforeNonExistent[0].id)
        assertEquals(Snowflake(10), beforeNonExistent[4].id)

        // Query after non-existent message
        val afterNonExistent = cache.getMessages(Snowflake(1), after = Snowflake(11), limit = 5)
        assertEquals(5, afterNonExistent.size) // Should still return messages
        assertEquals(Snowflake(12), afterNonExistent[0].id)
        assertEquals(Snowflake(16), afterNonExistent[4].id)

        // Query around non-existent message
        val aroundNonExistent = cache.getMessages(Snowflake(1), around = Snowflake(11), limit = 5)
        assertEquals(5, aroundNonExistent.size) // Should still return messages
        assertEquals(Snowflake(8), aroundNonExistent[0].id)
        assertEquals(Snowflake(10), aroundNonExistent[2].id)
        assertEquals(Snowflake(13), aroundNonExistent[4].id)
    }

    @Test
    fun testQueryingWithNonContiguous() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        cache.addMessages(Snowflake(1), createConsecutiveMessages(0, 20, 5))

        // Query before non-existent message
        val beforeNonExistent = cache.getMessages(Snowflake(1), before = Snowflake(49), limit = 5)
        assertEquals(5, beforeNonExistent.size) // Should still return messages
        assertEquals(Snowflake(25), beforeNonExistent[0].id)
        assertEquals(Snowflake(45), beforeNonExistent[4].id)

        val afterNonExistent = cache.getMessages(Snowflake(1), after = Snowflake(51), limit = 5)
        assertEquals(5, afterNonExistent.size) // Should still return messages
        assertEquals(Snowflake(55), afterNonExistent[0].id)
        assertEquals(Snowflake(75), afterNonExistent[4].id)

        val aroundNonExistent = cache.getMessages(Snowflake(1), around = Snowflake(51), limit = 5)
        assertEquals(5, aroundNonExistent.size) // Should still return messages
        assertEquals(Snowflake(40), aroundNonExistent[0].id)
        assertEquals(Snowflake(50), aroundNonExistent[2].id)
        assertEquals(Snowflake(60), aroundNonExistent[4].id)
    }

    @Test
    fun testEdgeCasesInQueryParameters() {
        val cache = Cache()
        cache.registerMessageCacheFor(Snowflake(1))

        // Add some messages
        cache.addMessages(Snowflake(1), createConsecutiveMessages(1, 10))

        // Test with non-existent message ID
        val beforeNonExistent = cache.getMessages(Snowflake(1), before = Snowflake(999), limit = 5)
        assertEquals(5, beforeNonExistent.size) // Should still return messages

        val afterNonExistent = cache.getMessages(Snowflake(1), after = Snowflake(999), limit = 5)
        assertTrue(afterNonExistent.isEmpty()) // No messages after a non-existent high ID

        val aroundNonExistent = cache.getMessages(Snowflake(1), around = Snowflake(999), limit = 5)
        assertTrue(aroundNonExistent.isNotEmpty()) // Should find messages below this ID

        // Test with limit 0
        val zeroLimit = cache.getMessages(Snowflake(1), limit = 0)
        assertTrue(zeroLimit.isEmpty())
    }
}
