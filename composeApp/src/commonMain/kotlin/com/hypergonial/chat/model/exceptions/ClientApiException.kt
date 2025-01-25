package com.hypergonial.chat.model.exceptions

/** An exception that is thrown when an API request fails in the 400..499 range. */
open class ClientApiException(message: String? = null, cause: Throwable? = null) : ApiException(message, cause)
