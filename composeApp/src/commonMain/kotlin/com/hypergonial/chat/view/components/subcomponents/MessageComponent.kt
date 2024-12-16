package com.hypergonial.chat.view.components.subcomponents

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.payloads.Message

// Note to self: Subcomponents must not have navigation, StateKeeper, or InstanceKeeper,
// because they get the parent's ctx directly which is *technically* not a supported configuration.
// See https://arkivanov.github.io/Decompose/component/child-components/#adding-a-child-component-manually
// for more information.

interface MessageComponent {
    data class MessageUIState(
        val message: Message,
        val isPending: Boolean = false,
        val isEdited: Boolean = false,
        val isBeingEdited: Boolean = false
    )


    val data: Value<MessageUIState>

    fun getKey(): String

    fun onPendingChanged(isPending: Boolean)
    fun onEditRequested()
    fun onEditFinished()
    fun onEditCanceled()
    fun onMessageUpdate(newMessage: Message)
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

    override fun onPendingChanged(isPending: Boolean) {
        data.value = data.value.copy(isPending = isPending)
    }

    override fun onEditRequested() {
        TODO("Not yet implemented")
    }

    override fun onEditFinished() {
        TODO("Not yet implemented")
    }

    override fun onEditCanceled() {
        TODO("Not yet implemented")
    }

    // TODO: Should be invoked by parent to minimize subscribers
    override fun onMessageUpdate(newMessage: Message) {
        data.value = data.value.copy(message = newMessage)
    }

}
