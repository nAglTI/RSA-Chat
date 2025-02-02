package com.hypergonial.chat.model.payloads.gateway

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/** A message received or sent over the gateway. */
@OptIn(ExperimentalSerializationApi::class) @Serializable @JsonClassDiscriminator("event") sealed class GatewayMessage
