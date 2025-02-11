package com.hypergonial.chat.model.exceptions

import kotlinx.serialization.SerializationException

class InvalidPayloadException(message: String? = null, cause: SerializationException? = null) :
    ClientException(message, cause)
