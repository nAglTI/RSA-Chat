package com.hypergonial.chat.view.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
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
import com.hypergonial.chat.model.MessageUpdateEvent
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.removeRange
import com.hypergonial.chat.sanitized
import com.hypergonial.chat.view.components.subcomponents.DefaultMessageComponent
import com.hypergonial.chat.view.components.subcomponents.DefaultMessageEntryComponent
import com.hypergonial.chat.view.components.subcomponents.EndIndicator
import com.hypergonial.chat.view.components.subcomponents.EndOfMessages
import com.hypergonial.chat.view.components.subcomponents.LoadMoreMessagesIndicator
import com.hypergonial.chat.view.components.subcomponents.MessageComponent
import com.hypergonial.chat.view.components.subcomponents.MessageEntryComponent
import kotlinx.coroutines.launch

private const val MESSAGE_BATCH_SIZE = 100u

interface HomeComponent {
    val data: Value<HomeState>

    fun onLogoutClicked()
    fun onMoreMessagesRequested(lastMessage: Snowflake? = null, isAtTop: Boolean)
    fun onMessageSend()
    fun onEditLastMessage()
    fun onChatBarContentChanged(value: TextFieldValue)
    fun onMessageDelete(messageId: Snowflake)


    data class HomeState(
        val currentChannelId: Snowflake? = Snowflake(0u),
        // The value of the chat bar
        val chatBarValue: TextFieldValue = TextFieldValue(),
        // The list of message entries to display
        val messageEntries: SnapshotStateList<MessageEntryComponent>,
        val lastSentMessageId: Snowflake? = null,
        // The state of the lazy list that displays the messages
        val listState: LazyListState = LazyListState(),
        // If true, the bottom of the message list is no longer loaded
        val isCruising: Boolean = false,
        // The state of the navigation drawer
        val navDrawerState: DrawerState = DrawerState(DrawerValue.Closed)
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
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onMessageUpdate)
    }

    override fun onLogoutClicked() = onLogout()

    private fun messageComponent(
        message: Message, isPending: Boolean = false, isEdited: Boolean = false
    ): MessageComponent {
        return DefaultMessageComponent(ctx, client, message, isPending, isEdited)
    }

    private fun messageEntryComponent(
        messages: SnapshotStateList<MessageComponent>, endIndicator: EndIndicator? = null
    ): MessageEntryComponent {
        return DefaultMessageEntryComponent(ctx, client, messages, endIndicator)
    }

    private fun requestMessagesScrollingUp(lastMessage: Snowflake? = null) {
        println("Requesting messages scrolling up")

        scope.launch {
            val messages = client.fetchMessages(
                channelId = Snowflake(0u), before = lastMessage, limit = MESSAGE_BATCH_SIZE
            )

            val currentEntries = data.value.messageEntries

            val features = createMessageFeatures(messages)

            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE

            // Remove the EOF/LoadMore indicator
            currentEntries.last().setEndIndicator(null)
            if (lastMessage == null) {
                // Remove the placeholder feature that is added when opening a channel
                currentEntries.removeLast()
            }

            // Prepend messages to the list
            currentEntries.addAll(features)

            currentEntries.last().setEndIndicator(
                if (isEnd) EndOfMessages else LoadMoreMessagesIndicator(isAtTop = true)
            )

            // Drop elements from the bottom beyond 300 messages
            if (currentEntries.size.toUInt() > MESSAGE_BATCH_SIZE * 3u) {
                println("Dropping ${currentEntries.size.toUInt() - MESSAGE_BATCH_SIZE} messages from bottom")
                currentEntries.removeRange(0 until currentEntries.size - MESSAGE_BATCH_SIZE.toInt() * 3)
                currentEntries.first().setEndIndicator(LoadMoreMessagesIndicator(isAtTop = false))
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

            val currentEntries = data.value.messageEntries

            // If we can't fetch more, then drop the loading indicator that triggered this request
            if (messages.isEmpty()) {
                currentEntries.first().setEndIndicator(null)
                data.value = data.value.copy(isCruising = false)
                return@launch
            }

            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE

            val features = createMessageFeatures(messages)

            // Remove the EOF/LoadMore indicator
            currentEntries.first().setEndIndicator(null)

            // Append messages to the list
            currentEntries.addAll(0, features)
            // If not at end yet, add a loading indicator
            if (!isEnd) currentEntries.first().setEndIndicator(
                LoadMoreMessagesIndicator(isAtTop = false)
            )

            // Drop elements beyond from the top 300 messages to prevent memory leaks
            if (currentEntries.size.toUInt() > MESSAGE_BATCH_SIZE * 3u) {
                println("Dropping ${currentEntries.size.toUInt() - MESSAGE_BATCH_SIZE * 3u} messages from top")
                currentEntries.removeRange(
                    currentEntries.size - MESSAGE_BATCH_SIZE.toInt() until currentEntries.size
                )
                // Add a new loading indicator at the top to allow scrolling up
                currentEntries.last().setEndIndicator(LoadMoreMessagesIndicator(isAtTop = true))
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

        val currentEntries = data.value.messageEntries
        val lastMessage = currentEntries.firstOrNull()?.lastMessage()?.data?.value?.message

        // If we just received the message we recently sent, mark it as not pending
        if (newMessage.author.id == client.cache.ownUser?.id && !isPending) {
            currentEntries.flatMap { it.data.value.messages }
                .firstOrNull { it.data.value.message.nonce == newMessage.nonce }
                ?.onPendingEnd(newMessage)
            println("Marked message as not pending")
            return
        }

        // Group messages by author
        if (lastMessage?.author?.id == newMessage.author.id) {
            println("Appending message to last message entry")
            currentEntries.first().pushMessage(
                messageComponent(
                    newMessage, isPending = isPending
                )
            )
        } else {
            println("Creating new message entry")
            currentEntries.add(
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

    suspend fun onMessageUpdate(event: MessageUpdateEvent) {
        println("Received message update event")
        data.value.messageEntries.flatMap { it.data.value.messages }
            .firstOrNull { it.data.value.message.id == event.message.id }?.onMessageUpdate(event)

    }

    private fun createPendingMessage(content: String, nonce: String) {
        val msg = Message(
            Snowflake(0u),
            data.value.currentChannelId ?: error("No channel selected"),
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

    override fun onEditLastMessage() {
        val lastMessage = data.value.messageEntries
            .flatMap { it.data.value.messages }
            .lastOrNull { it.data.value.message.author.id == client.cache.ownUser?.id } ?: return

        lastMessage.onEditStart()
    }

    override fun onChatBarContentChanged(value: TextFieldValue) {
        data.value = data.value.copy(chatBarValue = value.sanitized())
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
