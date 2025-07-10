package com.hypergonial.chat

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hypergonial.chat.data.SecuredDataStoreImpl
import com.hypergonial.chat.data.SecurityDataUtilsImpl
import com.hypergonial.chat.model.AndroidSettings
import com.hypergonial.chat.model.payloads.toSnowflake
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.view.notificationProvider
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.serialization.json.Json

class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (settings !is AndroidSettings) {
            return
        }

        // TODO: use DI (koin) or extension Context.getSecuredDataStore()
        val securedDs = SecuredDataStoreImpl(
            "android_secured_ds",
            this.applicationContext,
            SecurityDataUtilsImpl(),
            Json.Default
        )

        settings.initialize(getSharedPreferences("settings", MODE_PRIVATE), securedDs)

        // Send local notification
        NotifierManager.initialize(
            configuration =
                NotificationPlatformConfiguration.Android(
                    notificationIconResId = R.drawable.ic_stat_chat_icon,
                    showPushNotification = false,
                )
        )
        NotifierManager.setLogger { Logger.withTag("NotifierManager").i(it) }

        if (message.data["type"] == "notification") {
            notificationProvider.sendNotification {
                channelId = message.data["channel_id"]?.toSnowflake()
                title = message.data["title"] ?: "You got mail!"
                body = message.data["body"] ?: "No content provided."
                payloadData = message.data
            }
        }
    }
}
