package com.hypergonial.chat.model.payloads.fcm

import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.toSnowflake
import com.mmk.kmpnotifier.notification.PayloadData

data class PushNotificationData(
    val channelId: Snowflake,
    val guildId: Snowflake,
) {
    companion object {
        fun fromPayload(payload: PayloadData): PushNotificationData {
            require(payload["type"]?.toString() == "notification") {
                "Invalid notification type"
            }

            return PushNotificationData(
                channelId = (payload["channel_id"] as String).toSnowflake(),
                guildId = (payload["guild_id"] as String).toSnowflake(),
            )
        }
    }
}
