package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextULong


interface HomeComponent {
    val data: Value<HomeState>

    fun onLogoutClicked()
    fun onMoreMessagesRequested(earliestMessage: Snowflake? = null)

    data class HomeState(val messages: List<MessageListFeature>)
}

class DefaultHomeComponent(
    private val ctx: ComponentContext,
    private val client: Client,
    private val onLogout: () -> Unit
) : HomeComponent, ComponentContext by ctx {
    private val scope = ctx.coroutineScope()

    override val data = MutableValue(HomeComponent.HomeState(messages = listOf(LoadMoreMessagesIndicator())))

    override fun onLogoutClicked() = onLogout()

    override fun onMoreMessagesRequested(earliestMessage: Snowflake?) {
        scope.launch {
            val messages = client.fetchMessages(channelId = Snowflake(0u), before = earliestMessage, limit = 100u)
            val features = createMessageFeatures(messages)

            val currentFeatures = data.value.messages.toMutableList()
            // Remove the EOF/LoadMore indicator
            currentFeatures.removeLast()

            // Append messages to the list
            currentFeatures.addAll(features)

            // Drop elements beyond 300 messages
            if (currentFeatures.size > 300) {
                println("Dropping ${currentFeatures.size - 300} messages")
                currentFeatures.drop(currentFeatures.size - 300)
                data.value = data.value.copy(messages = currentFeatures.drop(currentFeatures.size - 300))
            }
            else {
                data.value = data.value.copy(messages = currentFeatures)
            }
        }
    }
}

/** Creates a list of message list features from a list of messages.
 * This function groups messages by author and creates a list of message entries.
 *
 * @param messages The list of messages to create features from.
 * @param endOfMessages Whether we reached the end of the messages list.
 * @return A list of message list features.
 */
fun createMessageFeatures(
    messages: List<Message>, endOfMessages: Boolean = false
): List<MessageListFeature> {
    if (messages.isEmpty()) {
        // If we got an empty list of messages, we definitely ran out of messages.
        return listOf(EndOfMessages)
    }


    val entries = mutableListOf<MessageListFeature>()

    // TODO: Message grouping by author
    for (message in messages) {
        entries.add(MessageEntry(mutableListOf(message)))
    }

    entries.add(if (endOfMessages) EndOfMessages else LoadMoreMessagesIndicator())

    return entries
}

/** A "thing" that can appear in the messages list.
 * This can either be a message entry or a loading indicator, indicating the top of the messages list. */
sealed interface MessageListFeature {
    fun getKey(): ULong
}

/** A single message entry consists of multiple subsequent messages by the same author. */
class MessageEntry(val messages: MutableList<Message>) : MessageListFeature {
    override fun getKey(): ULong = messages.first().id.inner
}

/** A loading indicator that indicates the top of the messages list.
 * If this is rendered, it typically indicates that we ran out of messages and need to fetch more.
 *
 * @param wasSeen Whether the user has seen this loading indicator.
 */
data class LoadMoreMessagesIndicator(var wasSeen: Boolean = false) : MessageListFeature {
    // Value less than snowflakes from 2025 to ensure no conflict
    override fun getKey(): ULong = Random.nextULong(1u, 31557600000u)
}

/** A loading indicator that indicates the end of the messages list.
 * This should only be inserted in the list if we ran out of messages to fetch. */
data object EndOfMessages : MessageListFeature {
    override fun getKey(): ULong = 0u
}
