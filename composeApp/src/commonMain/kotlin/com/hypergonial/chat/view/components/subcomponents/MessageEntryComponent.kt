package com.hypergonial.chat.view.components.subcomponents

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake

interface MessageEntryComponent {

    val data: Value<MessageEntryState>

    data class MessageEntryState(
        val messages: SnapshotStateList<MessageComponent>, val endIndicator: EndIndicator? = null
    )

    fun getMessage(messageId: Snowflake): MessageComponent?

    fun lastMessage(): MessageComponent? {
        return data.value.messages.lastOrNull()
    }

    fun firstMessage(): MessageComponent? {
        return data.value.messages.firstOrNull()
    }

    fun pushMessage(message: MessageComponent) {
        data.value.messages.add(message)
    }

    fun getKey(): String {
        return data.value.messages.firstOrNull()?.getKey() ?: "emptykey"
    }

    fun setEndIndicator(endIndicator: EndIndicator?)

    fun updateMessage(messageId: Snowflake, newMessage: Message)
}

class DefaultMessageEntryComponent(
    val ctx: ComponentContext,
    val client: Client,
    messages: List<MessageComponent>,
    endIndicator: EndIndicator? = null
) : MessageEntryComponent {
    override val data = MutableValue(
        MessageEntryComponent.MessageEntryState(
            messages.toMutableStateList(),
            endIndicator
        )
    )

    override fun getMessage(messageId: Snowflake): MessageComponent? {
        return data.value.messages.find { it.data.value.message.id == messageId }
    }

    override fun updateMessage(messageId: Snowflake, newMessage: Message) {
        data.value.messages.find { it.data.value.message.id == messageId }?.onMessageUpdate(newMessage)
    }

    override fun setEndIndicator(endIndicator: EndIndicator?) {
        data.value = data.value.copy(endIndicator = endIndicator)
    }
}

sealed interface EndIndicator

/** A loading indicator that indicates the top of the messages list.
 * If this is rendered, it typically indicates that we ran out of messages and need to fetch more.
 *
 * @param wasSeen Whether the user has seen this loading indicator.
 */
data class LoadMoreMessagesIndicator(var wasSeen: Boolean = false, val isAtTop: Boolean = true) :
    EndIndicator

/** An indicator that indicates the end of the messages list.
 * This should only be inserted in the list if we ran out of messages to fetch. */
data object EndOfMessages : EndIndicator
