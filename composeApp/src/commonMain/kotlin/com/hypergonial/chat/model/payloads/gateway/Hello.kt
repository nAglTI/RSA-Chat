package com.hypergonial.chat.model.payloads.gateway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("HELLO") class Hello(val data: HelloPayload) : GatewayMessage()

@Serializable data class HelloPayload(@SerialName("heartbeat_interval") val heartbeatInterval: Long)
