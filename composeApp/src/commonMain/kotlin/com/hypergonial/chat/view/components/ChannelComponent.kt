package com.hypergonial.chat.view.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.EffectContainer
import com.hypergonial.chat.appendMessages
import com.hypergonial.chat.containAsEffect
import com.hypergonial.chat.genNonce
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.LifecycleResumedEvent
import com.hypergonial.chat.model.MessageCreateEvent
import com.hypergonial.chat.model.MessageRemoveEvent
import com.hypergonial.chat.model.MessageUpdateEvent
import com.hypergonial.chat.model.Mime
import com.hypergonial.chat.model.exceptions.ClientException
import com.hypergonial.chat.model.getMimeType
import com.hypergonial.chat.model.payloads.Attachment
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.prependMessages
import com.hypergonial.chat.removeFirstMessages
import com.hypergonial.chat.removeLastMessages
import com.hypergonial.chat.sanitized
import com.hypergonial.chat.totalMessageCount
import com.hypergonial.chat.view.components.subcomponents.DefaultMessageComponent
import com.hypergonial.chat.view.components.subcomponents.DefaultMessageEntryComponent
import com.hypergonial.chat.view.components.subcomponents.EndIndicator
import com.hypergonial.chat.view.components.subcomponents.EndOfMessages
import com.hypergonial.chat.view.components.subcomponents.LoadMoreMessagesIndicator
import com.hypergonial.chat.view.components.subcomponents.MessageComponent
import com.hypergonial.chat.view.components.subcomponents.MessageEntryComponent
import com.hypergonial.chat.view.content.ChannelContent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// Note: Do not raise this above 100 as the API will never return more than 100 messages at a time
// but the client will incorrectly assume that it got less messages than it requested
private const val MESSAGE_BATCH_SIZE = 100u

interface ChannelComponent : MainContentComponent, Displayable {
    val data: Value<ChannelState>

    /** Callback called when the logout button is clicked. */
    fun onLogoutClicked()

    /**
     * Callback called when the user requests more messages by scrolling the list.
     *
     * @param lastMessage The ID of the last message to fetch messages before or after.
     * @param isAtTop If true, the user is requesting messages at the top of the list.
     */
    fun onMoreMessagesRequested(lastMessage: Snowflake? = null, isAtTop: Boolean)

    /** Callback called when the user hits the send message button. */
    fun onMessageSend()

    /** Callback called when the user inputs the "edit last message" action. */
    fun onEditLastMessage()

    /**
     * Callback called when the user requests to attach a file.
     *
     * @param isMedia If true, the user is requesting to upload an image or video. This controls the file picker type.
     */
    fun onFileAttachRequested(isMedia: Boolean = false)

    /** Callback called when the user requests to open the file upload dropdown. */
    fun onFileUploadDropdownOpen()

    /** Callback called when the user requests to close the file upload dropdown. */
    fun onFileUploadDropdownClose()

    /** Callback called when the user drops files into the attachment drop target. */
    fun onFilesDropped(files: List<PlatformFile>)

    /** Callback called when the user requests to remove a pending file. */
    fun onPendingFileCancel(file: PlatformFile)

    /**
     * Callback called when the user changes the content of the chat bar.
     *
     * @param value The new value of the chat bar.
     */
    fun onChatBarContentChanged(value: TextFieldValue)

    /**
     * Callback called when the user requests to delete a message.
     *
     * @param messageId The ID of the message to delete.
     */
    fun onMessageDeleteRequested(messageId: Snowflake)

    @Composable override fun Display() = ChannelContent(this)

    data class ChannelState(
        /** The value of the chat bar */
        val chatBarValue: TextFieldValue = TextFieldValue(),
        /** Attachments awaiting upload */
        val pendingAttachments: SnapshotStateList<PlatformFile> = mutableStateListOf(),
        /** The cumulative file size of all pending attachments */
        val cumulativeFileSize: Long = 0,
        /** The list of message entries to display */
        var messageEntries: SnapshotStateList<MessageEntryComponent>,
        /** The ID of the last message sent by the user */
        val lastSentMessageId: Snowflake? = null,
        /** The state of the lazy list that displays the messages */
        val listState: LazyListState = LazyListState(),
        /** If true, the bottom of the message list is no longer loaded */
        val isCruising: Boolean = false,
        /** If true, the file upload dropdown is open */
        val isFileUploadDropdownOpen: Boolean = false,
        /** The message to be displayed in the snackbar */
        val snackbarMessage: EffectContainer<String> = "".containAsEffect(),
    )
}

/**
 * The default channel component implementation.
 *
 * @param ctx The component context.
 * @param client The client to use for API operations.
 * @param channelId The ID of the channel to display.
 * @param onLogout The callback to call when the user logs out. Includes the http URL of the asset.
 */
class DefaultChannelComponent(
    private val ctx: ComponentContext,
    private val client: Client,
    private val channelId: Snowflake,
    private val onLogout: () -> Unit,
) : ChannelComponent, ComponentContext by ctx {
    private val scope = ctx.coroutineScope()
    private val logger = KotlinLogging.logger {}

    override val data =
        MutableValue(
            ChannelComponent.ChannelState(
                messageEntries =
                    mutableStateListOf(
                        messageEntryComponent(mutableStateListOf(), topEndIndicator = LoadMoreMessagesIndicator())
                    )
            )
        )

    init {
        client.eventManager.apply {
            subscribeWithLifeCycle(ctx.lifecycle, ::onMessageCreate)
            subscribeWithLifeCycle(ctx.lifecycle, ::onMessageUpdate)
            subscribeWithLifeCycle(ctx.lifecycle, ::onMessageDelete)
            subscribeWithLifeCycle(ctx.lifecycle, ::onResume)
        }
    }

    override fun onLogoutClicked() = onLogout()

    /** The number of messages currently visible in the UI. This is used to determine when to page out messages. */
    private fun visibleMessageCount(): UInt = data.value.listState.layoutInfo.visibleItemsInfo.size.toUInt()

    /** The maximum number of messages to keep in memory. */
    private fun maximumMessageCount(): UInt = max(visibleMessageCount(), MESSAGE_BATCH_SIZE * 3u)

    /**
     * Creates a new message component from a message.
     *
     * @param message The message to create a component from.
     * @param isPending Whether the message is pending or not.
     * @param hasUploadingAttachments Whether the message has attachments that are currently being uploaded.
     * @return The message component.
     */
    private fun messageComponent(
        message: Message,
        isPending: Boolean = false,
        hasUploadingAttachments: Boolean = false,
    ): MessageComponent {
        return DefaultMessageComponent(ctx, client, message, isPending, hasUploadingAttachments)
    }

    /**
     * Create dummy attachments from a list of files.
     *
     * This is only used while the message is pending to get an accurate enough reading of attachments.
     *
     * @param files The list of files to create attachments from.
     * @return The list of attachments.
     */
    private fun makeDummyAttachments(files: List<PlatformFile>): List<Attachment> {
        return files.mapIndexed { i, file -> Attachment(i, file.name, file.name.getMimeType() ?: Mime.default()) }
    }

    /**
     * Creates a new message entry component from a list of messages.
     *
     * @param messages The list of messages to create a component from.
     * @param topEndIndicator The indicator to display at the top of the list.
     * @param bottomEndIndicator The indicator to display at the bottom of the list.
     * @return The message entry component.
     */
    private fun messageEntryComponent(
        messages: SnapshotStateList<MessageComponent>,
        topEndIndicator: EndIndicator? = null,
        bottomEndIndicator: LoadMoreMessagesIndicator? = null,
    ): MessageEntryComponent {
        return DefaultMessageEntryComponent(
            ctx,
            client,
            messages,
            topEndIndicator,
            bottomEndIndicator,
            onEndReached = ::onMoreMessagesRequested,
        )
    }

    /**
     * Requests more messages from the server.
     *
     * @param lastMessage The ID of the last message to fetch messages before.
     */
    private fun requestMessagesScrollingUp(lastMessage: Snowflake? = null) {
        scope.launch {
            logger.info { "Requesting more messages before $lastMessage..." }

            val messages =
                try {
                    client.fetchMessages(channelId = channelId, before = lastMessage, limit = MESSAGE_BATCH_SIZE)
                } catch (e: ClientException) {
                    data.value =
                        data.value.copy(snackbarMessage = "Failed to fetch messages: ${e.message}".containAsEffect())
                    return@launch
                }

            val currentEntries = data.value.messageEntries
            val features = createMessageFeatures(messages)
            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE

            // Remove the EOF/LoadMore indicator
            currentEntries.last().setTopEndIndicator(null)

            if (lastMessage == null) {
                // Remove the placeholder feature that is added when opening a channel
                currentEntries.removeLast()
            }

            // Append messages to the list
            currentEntries.appendMessages(features.reversed())

            // Edge-case when the channel is empty
            if (currentEntries.isEmpty()) {
                currentEntries.add(messageEntryComponent(mutableStateListOf(), topEndIndicator = EndOfMessages))
                return@launch
            }

            currentEntries.last().setTopEndIndicator(if (isEnd) EndOfMessages else LoadMoreMessagesIndicator())

            // Drop elements from the bottom beyond 300 messages
            if (currentEntries.totalMessageCount().toUInt() > maximumMessageCount() * 3u) {
                val dropCount = currentEntries.totalMessageCount() - maximumMessageCount().toInt() * 3

                logger.info { "Dropping $dropCount messages from the bottom" }
                currentEntries.removeFirstMessages(dropCount)
                currentEntries.first().setBottomEndIndicator(LoadMoreMessagesIndicator())
                data.value = data.value.copy(isCruising = true)
            }
        }
    }

    /**
     * Requests more messages from the server.
     *
     * @param firstMessage The ID of the last message to fetch messages after.
     */
    private fun requestMessagesScrollingDown(firstMessage: Snowflake? = null) {
        scope.launch {
            logger.info { "Requesting more messages after $firstMessage..." }

            val messages =
                try {
                    client.fetchMessages(channelId = channelId, after = firstMessage, limit = MESSAGE_BATCH_SIZE)
                } catch (e: ClientException) {
                    data.value =
                        data.value.copy(snackbarMessage = "Failed to fetch messages: ${e.message}".containAsEffect())
                    return@launch
                }

            val currentEntries = data.value.messageEntries
            val isEnd = messages.size.toUInt() < MESSAGE_BATCH_SIZE
            val features = createMessageFeatures(messages)

            // Remove the EOF/LoadMore indicator that triggered this request
            currentEntries.firstOrNull()?.setBottomEndIndicator(null)

            currentEntries.prependMessages(features.reversed())

            if (isEnd) {
                // Leave cruising mode if we reached the last chunk of messages
                data.value = data.value.copy(isCruising = false)
            } else {
                // If not at end yet, add a loading indicator at the bottom
                currentEntries.first().setBottomEndIndicator(LoadMoreMessagesIndicator())
            }

            data.value.listState.layoutInfo.visibleItemsInfo.size

            // Drop elements beyond from the top 300 messages to prevent memory leaks
            if (currentEntries.totalMessageCount().toUInt() > maximumMessageCount() * 3u) {
                val dropCount = currentEntries.totalMessageCount() - maximumMessageCount().toInt() * 3

                logger.info { "Dropping $dropCount messages from the top" }
                currentEntries.removeLastMessages(dropCount)
                // Add a new loading indicator at the top to allow scrolling up
                currentEntries.last().setTopEndIndicator(LoadMoreMessagesIndicator())
            }
        }
    }

    /**
     * Refresh the message list completely by fetching the latest messages from the server around the message centered
     * on screen. Discards all messages currently in the list and replaces them with the new ones. If all goes well, the
     * user should not notice any changes.
     */
    private fun refreshMessageList() {
        scope.launch {
            logger.info { "Refreshing message list..." }
            // Determine message at the center of the screen
            val visibleItems = data.value.listState.layoutInfo.visibleItemsInfo
            val centerIndex = visibleItems.getOrNull(visibleItems.size / 2)?.index
            val entry = data.value.messageEntries.getOrNull(centerIndex ?: 0)
            val entryCenterIndex = entry?.data?.value?.messages?.size?.div(2) ?: 0
            val middle = entry?.data?.value?.messages?.getOrNull(entryCenterIndex)?.data?.value?.message

            // Fetch messages around it
            val messages =
                try {
                    client.fetchMessages(channelId = channelId, around = middle?.id, limit = MESSAGE_BATCH_SIZE)
                } catch (e: ClientException) {
                    data.value =
                        data.value.copy(snackbarMessage = "Failed to refresh messages: ${e.message}".containAsEffect())
                    return@launch
                }

            // Note: middle message may not exist anymore (if it was deleted), do not use !=
            val topMessages = messages.takeWhile { middle?.id?.let { it1 -> it.id < it1 } == true }
            val bottomMessages = messages.dropWhile { middle?.id?.let { it1 -> it.id < it1 } == true }.drop(1)

            val entries = createMessageFeatures(messages).reversed()

            // Figure out if either end is a possible end of the channel
            val isTopEnd = topMessages.size.toUInt() < (MESSAGE_BATCH_SIZE / 2u) - 1u
            val isBottomEnd = bottomMessages.size.toUInt() < (MESSAGE_BATCH_SIZE / 2u) - 1u

            if (!isTopEnd) {
                entries.last().setTopEndIndicator(LoadMoreMessagesIndicator())
            } else {
                entries.last().setTopEndIndicator(EndOfMessages)
            }

            if (!isBottomEnd) {
                entries.first().setBottomEndIndicator(LoadMoreMessagesIndicator())
            }

            data.value = data.value.copy(messageEntries = entries.toMutableStateList(), isCruising = !isBottomEnd)
        }
    }

    override fun onMoreMessagesRequested(lastMessage: Snowflake?, isAtTop: Boolean) {
        if (isAtTop) {
            requestMessagesScrollingUp(lastMessage)
        } else {
            requestMessagesScrollingDown(lastMessage)
        }
    }

    override fun onFileUploadDropdownOpen() {
        data.value = data.value.copy(isFileUploadDropdownOpen = true)
    }

    override fun onFileUploadDropdownClose() {
        data.value = data.value.copy(isFileUploadDropdownOpen = false)
    }

    override fun onFileAttachRequested(isMedia: Boolean) {
        onFileUploadDropdownClose()

        scope.launch {
            val file =
                if (isMedia) {
                    FileKit.pickFile(PickerType.ImageAndVideo, PickerMode.Single, title = "Select media to upload")
                        ?: return@launch
                } else {
                    FileKit.pickFile(PickerType.File(), PickerMode.Single, title = "Select a file to upload")
                        ?: return@launch
                }

            val size = file.getSize() ?: 0

            if (data.value.cumulativeFileSize + size > 8 * 1024 * 1024) {
                data.value =
                    data.value.copy(
                        snackbarMessage = "Upload size exceeds 8MB, cannot upload more files".containAsEffect()
                    )
                return@launch
            }

            data.value.pendingAttachments.add(file)
        }
    }

    override fun onFilesDropped(files: List<PlatformFile>) {
        files.forEach {
            val size = it.getSize() ?: 0

            if (data.value.cumulativeFileSize + size > 8 * 1024 * 1024) {
                data.value =
                    data.value.copy(
                        snackbarMessage = "Upload size exceeds 8MB, cannot upload more files".containAsEffect()
                    )
                return
            }

            data.value.pendingAttachments.add(it)
            data.value = data.value.copy(cumulativeFileSize = data.value.cumulativeFileSize + size)
        }
    }

    override fun onPendingFileCancel(file: PlatformFile) {
        data.value.pendingAttachments.remove(file)
        data.value = data.value.copy(cumulativeFileSize = data.value.cumulativeFileSize - (file.getSize() ?: 0))
    }

    /**
     * Add a new message to the list of message entries.
     *
     * @param newMessage The message to add to the list.
     * @param isPending Whether the message is pending or not. A pending message was sent by the user but not yet
     *   received by the server.
     */
    private fun addMessage(newMessage: Message, isPending: Boolean = false, hasUploadingAttachments: Boolean = false) {
        if (data.value.isCruising) return

        val currentEntries = data.value.messageEntries
        val lastMessage = currentEntries.firstOrNull()?.lastMessage()?.data?.value?.message
        val lastCreatedAt = currentEntries.firstOrNull()?.lastMessage()?.createdAt() ?: Instant.DISTANT_PAST

        // If we just received the message we recently sent, mark it as not pending
        if (
            newMessage.author.id == client.cache.ownUser?.id &&
                !isPending
                // Check if the session_id is the same as the one that sent the message
                &&
                newMessage.nonce?.split(".")?.get(0) == client.sessionId
        ) {
            currentEntries
                .flatMap { it.data.value.messages }
                .firstOrNull { it.data.value.message.nonce == newMessage.nonce }
                ?.onPendingEnd(newMessage)
            return
        }

        // Group recent messages by author
        if (
            lastMessage?.author?.id == newMessage.author.id &&
                Clock.System.now() - lastCreatedAt < 5.minutes &&
                currentEntries.lastOrNull()?.data?.value?.messages?.size?.let { it < 100 } == true
        ) {
            currentEntries.first().pushMessage(messageComponent(newMessage, isPending, hasUploadingAttachments))
        } else {
            currentEntries.add(
                0,
                messageEntryComponent(
                    mutableStateListOf(messageComponent(newMessage, isPending, hasUploadingAttachments))
                ),
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

    private fun onMessageCreate(event: MessageCreateEvent) {
        // If the user is so high up that the messages at the bottom aren't even loaded, just don't
        // bother
        if (event.message.channelId != channelId || data.value.isCruising) {
            // TODO: Implement a way to notify the user that new messages are available
            return
        }

        addMessage(event.message, isPending = false)
    }

    private fun onMessageUpdate(event: MessageUpdateEvent) {
        if (event.message.channelId != channelId) return

        data.value.messageEntries
            .flatMap { it.data.value.messages }
            .firstOrNull { it.data.value.message.id == event.message.id }
            ?.onMessageUpdate(event)
    }

    private fun onMessageDelete(event: MessageRemoveEvent) {
        if (event.channelId != channelId) return

        val entry = data.value.messageEntries.firstOrNull { it.containsMessage(event.id) } ?: return
        entry.removeMessage(event.id)

        if (entry.isEmpty()) {
            data.value.messageEntries.remove(entry)
        }
    }

    @Suppress("UnusedParameter")
    private fun onResume(event: LifecycleResumedEvent) {
        refreshMessageList()
    }

    /**
     * Add a dummy message to display in the UI while the message is pending.
     *
     * @param content The content of the message.
     * @param nonce The nonce of the message.
     * @param attachments The attachments of the message. These may be used for placeholders to display upload progress.
     */
    private fun addPendingMessage(content: String, nonce: String, attachments: List<PlatformFile>) {
        val msg =
            Message(
                Snowflake(0u),
                channelId,
                client.cache.ownUser ?: error("Own user not cached, cannot send message (This is a bug)"),
                content = content,
                nonce = nonce,
                attachments = makeDummyAttachments(attachments),
            )
        addMessage(msg, isPending = true, hasUploadingAttachments = attachments.isNotEmpty())
    }

    override fun onMessageSend() {
        val content = data.value.chatBarValue.text.trim()
        val attachments = data.value.pendingAttachments.toList()

        if (content.isBlank() && attachments.isEmpty()) return

        scope.launch {
            val nonce = genNonce(client.sessionId)
            data.value.pendingAttachments.clear()
            data.value = data.value.copy(chatBarValue = data.value.chatBarValue.copy(text = ""), cumulativeFileSize = 0)
            addPendingMessage(content, nonce, attachments = attachments)
            try {
                client.sendMessage(channelId, content = content, nonce = nonce, attachments = attachments)
            } catch (e: ClientException) {
                data.value.messageEntries
                    .flatMap { it.data.value.messages }
                    .firstOrNull { it.data.value.message.nonce == nonce }
                    ?.onFailed()
                data.value = data.value.copy(snackbarMessage = "Failed to send message: ${e.message}".containAsEffect())
                logger.error { "Failed to send message: ${e.message}" }
                e.printStackTrace()
            }
        }
    }

    override fun onEditLastMessage() {
        val lastMessage =
            data.value.messageEntries
                .flatMap { it.data.value.messages }
                .lastOrNull { it.data.value.message.author.id == client.cache.ownUser?.id } ?: return

        lastMessage.onEditStart()
    }

    override fun onMessageDeleteRequested(messageId: Snowflake) {
        scope.launch {
            try {
                client.deleteMessage(channelId, messageId)
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(snackbarMessage = "Failed to delete message: ${e.message}".containAsEffect())
                logger.error { "Failed to delete message: ${e.message}" }
            }
        }
    }

    override fun onChatBarContentChanged(value: TextFieldValue) {
        data.value = data.value.copy(chatBarValue = value.sanitized())
    }

    /**
     * Creates a list of message list features from a list of messages. This function groups messages by author and
     * creates a list of message entries.
     *
     * @param messages The list of messages to create features from.
     * @return A list of message list features.
     */
    private fun createMessageFeatures(messages: List<Message>): List<MessageEntryComponent> {
        if (messages.isEmpty()) {
            return emptyList()
        }

        val entries = mutableListOf<MessageEntryComponent>()

        // Group messages by author
        for (message in messages) {
            if (
                entries.isNotEmpty() &&
                    entries.last().author?.id == message.author.id &&
                    message.createdAt - entries.last().lastMessage()!!.data.value.message.createdAt < 5.minutes
            ) {
                entries.last().pushMessage(messageComponent(message))
            } else {
                entries.add(messageEntryComponent(mutableStateListOf(messageComponent(message))))
            }
        }

        return entries
    }
}
