package com.hypergonial.chat.model

import com.hypergonial.chat.model.payloads.Message

open class Event

open class MessageEvent(val message: Message) : Event()

class MessageCreateEvent(message: Message) : MessageEvent(message)

class MessageUpdateEvent(message: Message) : MessageEvent(message)
