package com.hypergonial.chat.view.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.genNonce
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.MessageCreateEvent
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.removeRange
import kotlinx.coroutines.launch

private const val MESSAGE_BATCH_SIZE = 100u

interface HomeComponent {
    val data: Value<HomeState>

    fun onLogoutClicked()
    fun onMoreMessagesRequested(lastMessage: Snowflake? = null, isAtTop: Boolean)
    fun onMessageSend()
    fun onChatBarContentChanged(value: TextFieldValue)

    data class HomeState(
        val currentChannelId: Snowflake? = null,
        val chatBarValue: TextFieldValue = TextFieldValue(),
        val messages: ArrayDeque<MessageEntry>,
        val lastSentMessageId: Snowflake? = null,
        val listState: LazyListState = LazyListState(),
        val isCruising: Boolean = false
    )
}

class DefaultHomeComponent(
    private val ctx: ComponentContext, private val client: Client, private val onLogout: () -> Unit
) : HomeComponent, ComponentContext by ctx {
    private val scope = ctx.coroutineScope()

    override val data = MutableValue(
        HomeComponent.HomeState(
            messages = ArrayDeque(
                listOf(
                    MessageEntry(mutableStateListOf(), LoadMoreMessagesIndicator(isAtTop = true))
                )
            )
        )
    )

    init {
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onMessageCreate)
    }

    override fun onLogoutClicked() = onLogout()

    private fun requestMessagesScrollingUp(lastMessage: Snowflake? = null) {
        scope.launch {
            val messages = client.fetchMessages(
                channelId = Snowflake(0u), before = lastMessage, limit = MESSAGE_BATCH_SIZE
            )

            val currentFeatures = ArrayDeque(data.value.messages)

            val features = createMessageFeatures(messages)

            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE

            // Remove the EOF/LoadMore indicator
            currentFeatures.first().endIndicator = null
            if (lastMessage == null) {
                // Remove the placeholder feature that is added when opening a channel
                currentFeatures.removeFirst()
            }

            // Prepend messages to the list
            currentFeatures.addAll(0, features)

            currentFeatures.first().endIndicator =
                if (isEnd) EndOfMessages else LoadMoreMessagesIndicator(isAtTop = true)

            // Drop elements from the bottom beyond 300 messages
            if (currentFeatures.size.toUInt() > MESSAGE_BATCH_SIZE * 3u) {
                println("Dropping ${currentFeatures.size.toUInt() - MESSAGE_BATCH_SIZE} messages from bottom")
                currentFeatures.removeRange(
                    currentFeatures.size - MESSAGE_BATCH_SIZE.toInt() until currentFeatures.size
                )
                currentFeatures.last().endIndicator = LoadMoreMessagesIndicator(isAtTop = false)
                data.value = data.value.copy(messages = currentFeatures, isCruising = true)
            } else {
                data.value = data.value.copy(messages = currentFeatures)
            }
            if (lastMessage == null) {
                // Start at the bottom if we just opened the channel
                data.value.listState.requestScrollToItem(features.size - 1)
            }
        }
    }

    private fun requestMessagesScrollingDown(lastMessage: Snowflake? = null) {
        scope.launch {
            val messages = client.fetchMessages(
                channelId = Snowflake(0u), after = lastMessage, limit = MESSAGE_BATCH_SIZE
            )

            val currentFeatures = ArrayDeque(data.value.messages)

            // If we can't fetch more, then drop the loading indicator that triggered this request
            if (messages.isEmpty()) {
                currentFeatures.last().endIndicator = null
                data.value = data.value.copy(messages = currentFeatures, isCruising = false)
                return@launch
            }

            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE

            val features = createMessageFeatures(messages)

            // Remove the EOF/LoadMore indicator
            currentFeatures.last().endIndicator = null

            // Append messages to the list
            currentFeatures.addAll(features)
            // If not at end yet, add a loading indicator
            if (!isEnd) currentFeatures.last().endIndicator =
                LoadMoreMessagesIndicator(isAtTop = false)

            // Drop elements beyond from the top 300 messages to prevent memory leaks
            if (currentFeatures.size.toUInt() > MESSAGE_BATCH_SIZE * 3u) {
                println("Dropping ${currentFeatures.size.toUInt() - MESSAGE_BATCH_SIZE * 3u} messages from top")

                currentFeatures.removeRange(0 until currentFeatures.size - MESSAGE_BATCH_SIZE.toInt() * 3)
                // Add a new loading indicator at the top to allow scrolling up
                currentFeatures.first().endIndicator = LoadMoreMessagesIndicator(isAtTop = true)
                data.value = data.value.copy(messages = currentFeatures, isCruising = data.value.isCruising && !isEnd)
            } else {
                data.value = data.value.copy(messages = currentFeatures, isCruising = data.value.isCruising && !isEnd)
            }
        }
    }

    override fun onMoreMessagesRequested(lastMessage: Snowflake?, isAtTop: Boolean) {
        if (isAtTop) {
            requestMessagesScrollingUp(lastMessage)
        } else {
            requestMessagesScrollingDown(lastMessage)
        }
    }

    /** Add a new message to the list of message entries.
     *
     * @param newMessage The message to add to the list.
     * @param isPending Whether the message is pending or not.
     * A pending message was sent by the user but not yet received by the server.
     */
    private fun addMessage(newMessage: Message, isPending: Boolean = false) {
        if (data.value.isCruising) return

        val currentFeatures = ArrayDeque(data.value.messages)
        val lastMessage = currentFeatures.lastOrNull()?.messages?.lastOrNull()?.message

        // If we just received the message we recently sent, mark it as not pending
        if (newMessage.author.id == client.cache.ownUser?.id && !isPending) {
            for (feature in currentFeatures) {
                for ((i, message) in feature.messages.withIndex()) {
                    if (message.message.nonce == newMessage.nonce) {
                        feature.messages[i] =
                            feature.messages[i].copy(message = newMessage, isPending = false)
                    }
                }
            }
            println("Marked message as not pending")
            data.value = data.value.copy(messages = currentFeatures)
            return
        }

        // Group messages by author
        if (lastMessage?.author?.id == newMessage.author.id) {
            println("Appending message to last message entry")
            currentFeatures.last().messages.add(MessageUIState(newMessage, isPending = isPending))
        } else {
            println("Creating new message entry")
            currentFeatures.add(
                MessageEntry(
                    mutableStateListOf(
                        MessageUIState(
                            newMessage, isPending = isPending
                        )
                    )
                )
            )
        }
        data.value = data.value.copy(messages = currentFeatures)

        val isAtBottom = (data.value.listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            ?: 0) >= data.value.messages.size - 2

        // If we just got a message and the UI is at the bottom, keep it there
        // Also if we just sent a message, scroll the UI down
        if (isAtBottom || newMessage.author.id == client.cache.ownUser?.id && !data.value.isCruising) {
            data.value.listState.requestScrollToItem(data.value.messages.size - 1)
        }
    }

    /** Invoked when a new message is created on the server. */
    suspend fun onMessageCreate(event: MessageCreateEvent) {
        println("Received message create event")
        // If the user is so high up that the messages at the bottom aren't even loaded, just don't bother
        if (data.value.isCruising) {
            // TODO: Implement a way to notify the user that new messages are available
            return
        }

        addMessage(event.message, isPending = false)
    }

    private fun createPendingMessage(content: String, nonce: String) {
        val msg = Message(
            Snowflake(0u),
            content,
            client.cache.ownUser ?: error("Own user not cached, cannot send message"),
            nonce,
        )
        addMessage(msg, isPending = true)
    }

    /** Invoked when the user hits the message send button. */
    override fun onMessageSend() {
        val content = data.value.chatBarValue.text.trim()

        if (content.isBlank()) return

        scope.launch {
            val nonce = genNonce()
            data.value = data.value.copy(chatBarValue = data.value.chatBarValue.copy(text = ""))
            createPendingMessage(content, nonce)
            client.sendMessage(channelId = Snowflake(0u), content = content, nonce = nonce)
        }
    }

    override fun onChatBarContentChanged(value: TextFieldValue) {
        data.value = data.value.copy(chatBarValue = value)
    }
}

/** Creates a list of message list features from a list of messages.
 * This function groups messages by author and creates a list of message entries.
 *
 * @param messages The list of messages to create features from.
 * @return A list of message list features.
 */
fun createMessageFeatures(
    messages: List<Message>
): List<MessageEntry> {
    if (messages.isEmpty()) {
        return emptyList()
    }


    val entries = mutableListOf<MessageEntry>()

    // TODO: Message grouping by author
    for (message in messages) {
        entries.add(MessageEntry(mutableStateListOf(MessageUIState(message))))
    }

    return entries
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


data class MessageUIState(val message: Message, val isPending: Boolean = false) {
    private val wasPending = isPending

    fun getKey(): String? = if (wasPending) message.nonce else message.id.toString()
}

/** A single message entry consists of multiple subsequent messages by the same author. */
class MessageEntry(
    val messages: SnapshotStateList<MessageUIState>, var endIndicator: EndIndicator? = null
) {
    fun getKey(): String = messages.firstOrNull()?.getKey() ?: "null"
}


