package com.hypergonial.chat.view.components.subcomponents

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.MessageUpdateEvent
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.sanitized
import kotlinx.coroutines.launch

// Note to self: Subcomponents must not have navigation, StateKeeper, or InstanceKeeper,
// because they get the parent's ctx directly which is *technically* not a supported configuration.
// See https://arkivanov.github.io/Decompose/component/child-components/#adding-a-child-component-manually
// for more information.

interface MessageComponent {
    data class MessageUIState(
        val message: Message,
        val isPending: Boolean = false,
        val isEdited: Boolean = false,
        val isBeingEdited: Boolean = false,
        val editorState: TextFieldValue = TextFieldValue()
    )


    val data: Value<MessageUIState>

    fun getKey(): String

    /** Invoked when the message is received by the backend server.
     *
     * @param message The message that was received and validated by the backend
     * */
    fun onPendingEnd(message: Message)

    /** Invoked when the user starts editing the message */
    fun onEditStart()

    /** Invoked when the user finishes editing the message */
    fun onEditFinish()

    /** Invoked when the user cancels editing the message */
    fun onEditCancel()

    /** Invoked when the user changes the editor state (types in the editor)
     *
     * @param value The new state of the editor
     * */
    fun onEditorStateChanged(value: TextFieldValue)

    /** Invoked when a message update event is received from the backend for this message
     *
     * @param event The event that was received
     * */
    fun onMessageUpdate(event: MessageUpdateEvent)
}

class DefaultMessageComponent(
    private val ctx: ComponentContext,
    private val client: Client,
    message: Message,
    isPending: Boolean = false,
    isEdited: Boolean = false,
) : MessageComponent {
    override val data = MutableValue(MessageComponent.MessageUIState(message, isPending, isEdited))
    private val wasCreatedAsPending = isPending

    override fun getKey(): String {
        return if (!wasCreatedAsPending) data.value.message.id.toString() else {
            data.value.message.nonce.toString()
        }
    }

    init {
        // TODO: Idea: If pending, do a waitFor with timeout HERE instead of in the top-level viewmodel
        // and then either set isPending to false or set a sendFailed flag
    }

    override fun onPendingEnd(message: Message) {
        println("Pending state ended, got ID from backend: ${message.id}")
        data.value = data.value.copy(isPending = false, message = message)
    }


    override fun onEditStart() {
        if (data.value.message.author.id != client.cache.ownUser?.id) {
            return
        }

        data.value = data.value.copy(
            isBeingEdited = true,
            editorState = TextFieldValue(
                data.value.message.content ?: "",
                selection = TextRange(data.value.message.content?.length ?: 0)
            )
        )
    }

    override fun onEditorStateChanged(value: TextFieldValue) {
        data.value = data.value.copy(editorState = value.sanitized())
    }

    override fun onEditFinish() {
        if (data.value.editorState.text == data.value.message.content || data.value.editorState.text.isBlank()) {
            data.value = data.value.copy(isBeingEdited = false)
            return
        }

        data.value = data.value.copy(isPending = true, isBeingEdited = false)
        ctx.coroutineScope().launch {
            client.editMessage(
                data.value.message.channelId,
                data.value.message.id,
                data.value.editorState.text
            )
        }
    }

    override fun onEditCancel() {
        data.value = data.value.copy(isBeingEdited = false, editorState = TextFieldValue())
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        data.value = data.value.copy(message = event.message, isEdited = true, isPending = false)
    }

}
