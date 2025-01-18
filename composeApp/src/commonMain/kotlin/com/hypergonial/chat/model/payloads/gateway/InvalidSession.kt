package com.hypergonial.chat.model.payloads.gateway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("INVALID_SESSION")
class InvalidSession(@SerialName("data") val reason: String) : GatewayMessage()
