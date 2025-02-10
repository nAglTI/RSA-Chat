package com.hypergonial.chat.view.components.subcomponents

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.payloads.PartialUser
import com.hypergonial.chat.model.payloads.Snowflake

/**
 * Represents a single entry in the message list.
 *
 * An entry is a grouping of messages sent by the same author in a short span of time (typically within 5 minutes).
 */
interface MessageEntryComponent {

    val data: Value<MessageEntryState>

    /** The author of the messages in this entry. This is assumed to be the same for all messages. */
    val author: PartialUser?
        get() = firstMessage()?.data?.value?.message?.author

    /** Returns the message with the given ID, if it exists. */
    fun getMessage(messageId: Snowflake): MessageComponent?

    /** Returns the last message in the entry, if it exists. */
    fun lastMessage(): MessageComponent? {
        return data.value.messages.lastOrNull()
    }

    /** Returns the first message in the entry, if it exists. */
    fun firstMessage(): MessageComponent? {
        return data.value.messages.firstOrNull()
    }

    /**
     * Pushes a message to the entry.
     *
     * @param message The message to push to the entry
     */
    fun pushMessage(message: MessageComponent) {
        require(author == null || message.data.value.message.author.id == author?.id) {
            "Message author does not match the author of the message entry."
        }

        data.value.messages.add(message)
    }

    /**
     * Checks if the entry contains a message with the given ID.
     *
     * @param messageId The ID of the message to check for
     * @return True if the entry contains the message, false otherwise
     */
    fun containsMessage(messageId: Snowflake): Boolean {
        return data.value.messages.any { it.data.value.message.id == messageId }
    }

    /**
     * Removes a message from the entry.
     *
     * @param messageId The ID of the message to remove
     */
    fun removeMessage(messageId: Snowflake) {
        val index = data.value.messages.indexOfFirst { it.data.value.message.id == messageId }
        if (index != -1) {
            data.value.messages.removeAt(index)
        }
    }

    /**
     * Returns the key of the message entry for use in lazy lists.
     *
     * @return The key of the message entry
     */
    fun getKey(): String

    fun onEndReached(isAtTop: Boolean)

    fun setTopEndIndicator(endIndicator: EndIndicator?)

    fun setBottomEndIndicator(endIndicator: LoadMoreMessagesIndicator?)

    data class MessageEntryState(
        /** The messages in this entry */
        val messages: SnapshotStateList<MessageComponent>,
        /** The end indicator for this entry, if any */
        val topEndIndicator: EndIndicator? = null,
        val bottomEndIndicator: LoadMoreMessagesIndicator? = null,
    )
}

/**
 * A default implementation of the [MessageEntryComponent] interface.
 *
 * @param ctx The component context
 * @param client The client to use for sending messages
 * @param messages The messages that this entry represents
 * @param endIndicator The end indicator for this entry, if any
 */
class DefaultMessageEntryComponent(
    val ctx: ComponentContext,
    val client: Client,
    messages: List<MessageComponent>,
    topEndIndicator: EndIndicator? = null,
    bottomEndIndicator: LoadMoreMessagesIndicator? = null,
    private val onEndReached: (Snowflake?, Boolean) -> Unit,
) : MessageEntryComponent {
    override val data =
        MutableValue(
            MessageEntryComponent.MessageEntryState(messages.toMutableStateList(), topEndIndicator, bottomEndIndicator)
        )
    // Ensure the entry key remains the same even if the messages change
    private val key = (messages.firstOrNull()?.data?.value?.message?.id ?: hashCode()).toString()

    override fun getKey(): String = key

    override fun getMessage(messageId: Snowflake): MessageComponent? {
        return data.value.messages.find { it.data.value.message.id == messageId }
    }

    override fun setTopEndIndicator(endIndicator: EndIndicator?) {
        data.value = data.value.copy(topEndIndicator = endIndicator)
    }

    override fun setBottomEndIndicator(endIndicator: LoadMoreMessagesIndicator?) {
        data.value = data.value.copy(bottomEndIndicator = endIndicator)
    }

    override fun onEndReached(isAtTop: Boolean) {
        val borderMsg = if (isAtTop) firstMessage() else lastMessage()
        onEndReached(borderMsg?.data?.value?.message?.id, isAtTop)
    }
}

/** An indicator that indicates the end of the messages list. */
sealed interface EndIndicator

/**
 * A loading indicator that indicates the top of the messages list. If this is rendered, it typically indicates that we
 * ran out of messages and need to fetch more.
 *
 * @param wasSeen Whether the user has seen this loading indicator.
 */
class LoadMoreMessagesIndicator : EndIndicator {
    var wasSeen: Boolean = false
}

/**
 * An indicator that indicates the end of the messages list. This should only be inserted in the list if there are no
 * more messages to fetch.
 */
data object EndOfMessages : EndIndicator
