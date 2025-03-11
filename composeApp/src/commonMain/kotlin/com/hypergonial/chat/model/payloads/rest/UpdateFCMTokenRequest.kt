package com.hypergonial.chat.model.payloads.rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateFCMTokenRequest(val token: String, @SerialName("previous_token") val previousToken: String? = null)
