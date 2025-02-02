package com.hypergonial.chat.model.exceptions

/** A base exception for all exceptions thrown by the client */
open class ClientException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
