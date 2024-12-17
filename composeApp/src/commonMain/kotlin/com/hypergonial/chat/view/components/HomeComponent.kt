package com.hypergonial.chat.view.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.TextRange
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
import com.hypergonial.chat.view.components.subcomponents.DefaultMessageComponent
import com.hypergonial.chat.view.components.subcomponents.DefaultMessageEntryComponent
import com.hypergonial.chat.view.components.subcomponents.EndIndicator
import com.hypergonial.chat.view.components.subcomponents.EndOfMessages
import com.hypergonial.chat.view.components.subcomponents.LoadMoreMessagesIndicator
import com.hypergonial.chat.view.components.subcomponents.MessageComponent
import com.hypergonial.chat.view.components.subcomponents.MessageEntryComponent
import kotlinx.coroutines.launch

private const val MESSAGE_BATCH_SIZE = 50u

interface HomeComponent {
    val data: Value<HomeState>

    fun onLogoutClicked()
    fun onMoreMessagesRequested(lastMessage: Snowflake? = null, isAtTop: Boolean)
    fun onMessageSend()
    fun onChatBarContentChanged(value: TextFieldValue)
    fun onMessageDelete(messageId: Snowflake)


    data class HomeState(
        val currentChannelId: Snowflake? = null,
        // The value of the chat bar
        val chatBarValue: TextFieldValue = TextFieldValue(),
        // The list of message entries to display
        val messageEntries: SnapshotStateList<MessageEntryComponent>,
        val lastSentMessageId: Snowflake? = null,
        // The state of the lazy list that displays the messages
        val listState: LazyListState = LazyListState(),
        // If true, the bottom of the message list is no longer loaded
        val isCruising: Boolean = false,
    )
}

class DefaultHomeComponent(
    private val ctx: ComponentContext, private val client: Client, private val onLogout: () -> Unit
) : HomeComponent, ComponentContext by ctx {
    private val scope = ctx.coroutineScope()

    override val data = MutableValue(
        HomeComponent.HomeState(
            messageEntries = mutableStateListOf(
                messageEntryComponent(
                    mutableStateListOf(), LoadMoreMessagesIndicator(isAtTop = true)
                )
            )
        )
    )

    init {
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onMessageCreate)
    }

    override fun onLogoutClicked() = onLogout()

    private fun messageComponent(
        message: Message, isPending: Boolean = false, isEdited: Boolean = false
    ): MessageComponent {/*val childCtx =
            childContext(key = "message-" + if (!isPending) message.id.toString() else message.nonce.toString())*/
        return DefaultMessageComponent(ctx, client, message, isPending, isEdited)
    }

    private fun messageEntryComponent(
        messages: SnapshotStateList<MessageComponent>, endIndicator: EndIndicator? = null
    ): MessageEntryComponent {/*val firstKey = messages.firstOrNull()?.getKey()
        val childCtx = childContext(key = "message-entry-$firstKey")*/
        return DefaultMessageEntryComponent(ctx, client, messages, endIndicator)
    }

    private fun requestMessagesScrollingUp(lastMessage: Snowflake? = null) {
        println("Requesting messages scrolling up")

        scope.launch {
            val messages = client.fetchMessages(
                channelId = Snowflake(0u), before = lastMessage, limit = MESSAGE_BATCH_SIZE
            )

            val currentFeatures = data.value.messageEntries

            val features = createMessageFeatures(messages)

            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE

            // Remove the EOF/LoadMore indicator
            currentFeatures.last().setEndIndicator(null)
            if (lastMessage == null) {
                // Remove the placeholder feature that is added when opening a channel
                currentFeatures.removeLast()
            }

            // Prepend messages to the list
            currentFeatures.addAll(features)

            currentFeatures.last().setEndIndicator(
                if (isEnd) EndOfMessages else LoadMoreMessagesIndicator(isAtTop = true)
            )

            // Drop elements from the bottom beyond 300 messages
            if (currentFeatures.size.toUInt() > MESSAGE_BATCH_SIZE * 3u) {
                println("Dropping ${currentFeatures.size.toUInt() - MESSAGE_BATCH_SIZE} messages from bottom")
                currentFeatures.removeRange(0 until currentFeatures.size - MESSAGE_BATCH_SIZE.toInt() * 3)
                currentFeatures.first().setEndIndicator(LoadMoreMessagesIndicator(isAtTop = false))
                data.value = data.value.copy(isCruising = true)
            }
        }
    }

    private fun requestMessagesScrollingDown(lastMessage: Snowflake? = null) {
        println("Requesting messages scrolling down after $lastMessage")

        scope.launch {
            val messages = client.fetchMessages(
                channelId = Snowflake(0u), after = lastMessage, limit = MESSAGE_BATCH_SIZE
            )

            println("Received ${messages.size} messages with the first having id ${messages.firstOrNull()?.id}")

            val currentFeatures = data.value.messageEntries

            // If we can't fetch more, then drop the loading indicator that triggered this request
            if (messages.isEmpty()) {
                currentFeatures.first().setEndIndicator(null)
                data.value = data.value.copy(isCruising = false)
                return@launch
            }

            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE

            val features = createMessageFeatures(messages)

            // Remove the EOF/LoadMore indicator
            currentFeatures.first().setEndIndicator(null)

            // Append messages to the list
            currentFeatures.addAll(0, features)
            // If not at end yet, add a loading indicator
            if (!isEnd) currentFeatures.first().setEndIndicator(
                LoadMoreMessagesIndicator(isAtTop = false)
            )

            // Drop elements beyond from the top 300 messages to prevent memory leaks
            if (currentFeatures.size.toUInt() > MESSAGE_BATCH_SIZE * 3u) {
                println("Dropping ${currentFeatures.size.toUInt() - MESSAGE_BATCH_SIZE * 3u} messages from top")
                currentFeatures.removeRange(
                    currentFeatures.size - MESSAGE_BATCH_SIZE.toInt() until currentFeatures.size
                )
                // Add a new loading indicator at the top to allow scrolling up
                currentFeatures.last().setEndIndicator(LoadMoreMessagesIndicator(isAtTop = true))
            }

            data.value = data.value.copy(isCruising = data.value.isCruising && !isEnd)
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

        val currentFeatures = data.value.messageEntries
        val lastMessage = currentFeatures.firstOrNull()?.lastMessage()?.data?.value?.message

        // If we just received the message we recently sent, mark it as not pending
        if (newMessage.author.id == client.cache.ownUser?.id && !isPending) {
            for (feature in currentFeatures) {
                for (comp in feature.data.value.messages) {
                    if (comp.data.value.message.nonce == newMessage.nonce) {
                        comp.onMessageUpdate(newMessage)
                        comp.onPendingChanged(false)
                    }
                }
            }
            println("Marked message as not pending")
            return
        }

        // Group messages by author
        if (lastMessage?.author?.id == newMessage.author.id) {
            println("Appending message to last message entry")
            currentFeatures.first().pushMessage(
                messageComponent(
                    newMessage, isPending = isPending
                )
            )
        } else {
            println("Creating new message entry")
            currentFeatures.add(
                0, messageEntryComponent(
                    mutableStateListOf(
                        messageComponent(
                            newMessage, isPending = isPending
                        )
                    )
                )
            )
        }

        val isAtBottom =
            data.value.listState.firstVisibleItemIndex == 0 && data.value.listState.firstVisibleItemScrollOffset == 0

        // If we just got a message and the UI is at the bottom, keep it there
        // Also if we just sent a message, scroll the UI down
        if (isAtBottom || newMessage.author.id == client.cache.ownUser?.id && !data.value.isCruising) {
            data.value.listState.requestScrollToItem(0, 0)
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

    /** Sanitize the text in the chat bar. */
    private fun sanitizeText(value: TextFieldValue): TextFieldValue {
        val tabCount = value.text.count { it == '\t' }
        val text = value.text.replace("\t", "    ")
        val selection =
            TextRange(value.selection.start + 3 * tabCount, value.selection.end + 3 * tabCount)
        return value.copy(text = text, selection = selection)
    }

    override fun onChatBarContentChanged(value: TextFieldValue) {
        data.value = data.value.copy(chatBarValue = sanitizeText(value))
    }

    override fun onMessageDelete(messageId: Snowflake) {
        TODO("Not yet implemented")
    }

    /** Creates a list of message list features from a list of messages.
     * This function groups messages by author and creates a list of message entries.
     *
     * @param messages The list of messages to create features from.
     * @return A list of message list features.
     */
    private fun createMessageFeatures(
        messages: List<Message>
    ): List<MessageEntryComponent> {
        if (messages.isEmpty()) {
            return emptyList()
        }


        val entries = mutableListOf<MessageEntryComponent>()

        // TODO: Message grouping by author
        for (message in messages) {
            entries.add(messageEntryComponent(mutableStateListOf(messageComponent(message))))
        }

        return entries
    }
}
