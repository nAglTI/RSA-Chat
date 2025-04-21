package com.hypergonial.chat.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable data class FCMSettings(val token: String, val lastUpdated: Instant)
