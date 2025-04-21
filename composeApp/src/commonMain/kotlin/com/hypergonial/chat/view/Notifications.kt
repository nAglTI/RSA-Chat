package com.hypergonial.chat.view

import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.settings
import com.mmk.kmpnotifier.notification.NotificationImage
import com.mmk.kmpnotifier.notification.NotifierBuilder
import com.mmk.kmpnotifier.notification.NotifierManager
import kotlin.random.Random

interface NotificationProvider {
    fun sendNotification(builder: NotificationBuilder.() -> Unit)

    fun dismissNotification(channelId: Snowflake, id: Int)

    fun dismissAllForChannel(channelId: Snowflake) {
        val ids = settings.getNotificationsIn(channelId)
        ids?.forEach { id -> dismissNotification(channelId, id) }
    }
}

class NotificationBuilder {
    var channelId: Snowflake? = null
    var id: Int = Random.nextInt(0, Int.MAX_VALUE)
    var title: String = ""
    var body: String = ""

    var payloadData: Map<String, String> = emptyMap()

    var image: NotificationImage? = null

    companion object {
        fun toKMPNotify(builder: NotificationBuilder): NotifierBuilder.() -> Unit {
            return {
                this.id = builder.id
                this.title = builder.title
                this.body = builder.body
                this.payloadData = builder.payloadData
                this.image = builder.image
            }
        }
    }
}

/**
 * Default notification provider that uses the KMP local notifier.
 *
 * This is used to send notifications to the user.
 */
object DefaultNotificationProvider : NotificationProvider {
    override fun sendNotification(builder: NotificationBuilder.() -> Unit) {
        val data = NotificationBuilder().apply(builder)
        val channelId = data.channelId

        require(channelId != null) { "Channel ID must be set" }

        NotifierManager.getLocalNotifier().notify(NotificationBuilder.toKMPNotify(data))
        settings.pushNotification(channelId, data.id)
    }

    override fun dismissNotification(channelId: Snowflake, id: Int) {
        NotifierManager.getLocalNotifier().remove(id)
        settings.popNotification(channelId, id)
    }
}

expect val notificationProvider: NotificationProvider
