package com.hypergonial.chat.view

import com.mmk.kmpnotifier.notification.NotifierBuilder

expect fun sendNotification(builder: NotifierBuilder.() -> Unit)
