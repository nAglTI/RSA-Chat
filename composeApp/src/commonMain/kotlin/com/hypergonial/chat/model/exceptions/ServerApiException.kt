package com.hypergonial.chat.model.exceptions

/** API exception that is thrown when an API request fails in the 500..599 range. */
open class ServerApiException(message: String? = null, cause: Throwable? = null) : ApiException(message, cause)
