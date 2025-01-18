package com.hypergonial.chat.model.payloads.gateway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("HEARTBEAT")
data object Heartbeat : GatewayMessage()
