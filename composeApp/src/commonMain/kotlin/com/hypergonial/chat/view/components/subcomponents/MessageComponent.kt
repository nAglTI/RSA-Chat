package com.hypergonial.chat.view.components.subcomponents

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.FocusAssetEvent
import com.hypergonial.chat.model.MessageUpdateEvent
import com.hypergonial.chat.model.UploadProgressEvent
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.sanitized
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// Note to self: Subcomponents must not have navigation, StateKeeper, or InstanceKeeper,
// because they get the parent's ctx directly which is *technically* not a supported configuration.
// See https://arkivanov.github.io/Decompose/component/child-components/#adding-a-child-component-manually
// for more information.

/** Represents the state of a single message in the message list. */
interface MessageComponent {

    val data: Value<MessageUIState>

    /**
     * Returns the key of the message component for use in lazy lists.
     *
     * @return The key of the message component
     */
    fun getKey(): String

    /**
     * Invoked when the message is received by the backend server.
     *
     * @param message The message that was received and validated by the backend
     */
    fun onPendingEnd(message: Message)

    /** Invoked when the message sending fails */
    fun onFailed()

    /** Invoked when the user starts editing the message */
    fun onEditStart()

    /** Invoked when the user finishes editing the message */
    fun onEditFinish()

    /** Invoked when the user cancels editing the message */
    fun onEditCancel()

    /** Invoked when the user tries to delete the message */
    fun onDeleteRequested()

    /**
     * Invoked when the user clicks on an attachment in the message
     *
     * @param id The ID of the attachment that was clicked
     */
    fun onAttachmentClicked(id: Int)

    /**
     * Invoked when the user changes the editor state (types in the editor)
     *
     * @param value The new state of the editor
     */
    fun onEditorStateChanged(value: TextFieldValue)

    /**
     * Invoked when a message update event is received from the backend for this message
     *
     * @param event The event that was received
     */
    fun onMessageUpdate(event: MessageUpdateEvent)

    data class MessageUIState(
        /** The message that this component represents */
        val message: Message,
        /** The time at which the message was created */
        val createdAt: Instant,
        /** Whether the message is pending */
        val isPending: Boolean = false,
        /** Whether the message sending failed */
        val isFailed: Boolean = false,
        /** Whether the message has attachments uploading or not */
        val hasUploadingAttachments: Boolean = false,
        /** Upload progress for the attachments (0.0..=1.0) */
        val uploadProgress: Double = 0.0,
        /** Whether the message editor is open or not */
        val isBeingEdited: Boolean = false,
        /** The state of the editor */
        val editorState: TextFieldValue = TextFieldValue(),
    )
}

/**
 * A default implementation of the [MessageComponent] interface.
 *
 * @param ctx The component context
 * @param client The client to use for sending messages
 * @param message The message that this component represents
 * @param isPending Whether the message is pending or not
 * @param hasUploadingAttachments Whether the message has uploading attachments or not
 */
class DefaultMessageComponent(
    private val ctx: ComponentContext,
    private val client: Client,
    message: Message,
    isPending: Boolean = false,
    hasUploadingAttachments: Boolean = false,
    isFailed: Boolean = false,
) : MessageComponent {
    override val data =
        MutableValue(
            MessageComponent.MessageUIState(
                message,
                createdAt = if (isPending) Clock.System.now() else message.createdAt,
                isPending = isPending,
                hasUploadingAttachments = hasUploadingAttachments,
                isFailed = isFailed,
            )
        )
    private val wasCreatedAsPending = isPending

    init {
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, this::onUploadStatusChange)
    }

    override fun getKey(): String {
        return if (!wasCreatedAsPending) data.value.message.id.toString()
        else {
            data.value.message.nonce.toString()
        }
    }

    override fun onPendingEnd(message: Message) {
        data.value =
            data.value.copy(
                isPending = false,
                hasUploadingAttachments = false,
                isFailed = false,
                message = message,
                createdAt = message.createdAt,
            )
        client.eventManager.unsubscribe(this::onUploadStatusChange)
    }

    override fun onFailed() {
        println("Failed")
        data.value = data.value.copy(isFailed = true, hasUploadingAttachments = false)
    }

    override fun onEditStart() {
        if (data.value.message.author.id != client.cache.ownUser?.id) {
            return
        }

        data.value =
            data.value.copy(
                isBeingEdited = true,
                editorState =
                    TextFieldValue(
                        data.value.message.content ?: "",
                        selection = TextRange(data.value.message.content?.length ?: 0),
                    ),
            )
    }

    override fun onEditorStateChanged(value: TextFieldValue) {
        data.value = data.value.copy(editorState = value.sanitized())
    }

    override fun onEditFinish() {
        if (data.value.editorState.text == data.value.message.content) {
            data.value = data.value.copy(isBeingEdited = false)
            return
        }

        if (data.value.editorState.text.isBlank() && data.value.message.attachments.isEmpty()) {
            onDeleteRequested()
            return
        }

        data.value = data.value.copy(isPending = true, isBeingEdited = false)
        ctx.coroutineScope().launch {
            client.editMessage(data.value.message.channelId, data.value.message.id, data.value.editorState.text)
        }
    }

    override fun onDeleteRequested() {
        if (data.value.message.author.id != client.cache.ownUser?.id) {
            // This would always 403, so we don't even try
            return
        }

        ctx.coroutineScope().launch {
            client.deleteMessage(data.value.message.channelId, data.value.message.id)
        }
    }

    /** Called when the upload status of the attachments attached to this message changes */
    private fun onUploadStatusChange(event: UploadProgressEvent) {
        if (data.value.message.nonce == event.nonce) {
            data.value = data.value.copy(uploadProgress = event.completionRate)
        }
    }

    override fun onAttachmentClicked(id: Int) {
        val url = data.value.message.attachments.find { it.id == id }?.makeUrl(data.value.message) ?: return
        client.eventManager.dispatch(FocusAssetEvent(url))
    }

    override fun onEditCancel() {
        data.value = data.value.copy(isBeingEdited = false, editorState = TextFieldValue())
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        data.value = data.value.copy(message = event.message, isPending = false)
    }
}
