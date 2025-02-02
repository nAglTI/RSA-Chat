package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Secret
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("IDENTIFY")
class Identify(val data: IdentifyPayload) : GatewayMessage() {
    constructor(token: Secret<String>) : this(IdentifyPayload(token))
}

@Serializable data class IdentifyPayload(val token: Secret<String>)
