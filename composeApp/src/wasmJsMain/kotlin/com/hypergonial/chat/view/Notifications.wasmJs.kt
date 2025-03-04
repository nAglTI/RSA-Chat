package com.hypergonial.chat.view

import com.mmk.kmpnotifier.notification.NotifierBuilder
import com.mmk.kmpnotifier.notification.NotifierManager

actual fun sendNotification(builder: NotifierBuilder.() -> Unit) {
    NotifierManager.getLocalNotifier().notify(builder)
}
