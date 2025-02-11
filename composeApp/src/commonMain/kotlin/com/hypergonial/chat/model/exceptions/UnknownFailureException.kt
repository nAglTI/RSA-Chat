package com.hypergonial.chat.model.exceptions

/** An unknown failure has occurred in the client. See the [cause] for more information. */
class UnknownFailureException(message: String? = null, cause: Throwable? = null) : ClientException(message, cause)
