package com.hypergonial.chat.model.exceptions

import kotlinx.io.IOException

/** A request has timed out. See [cause] on the specific timeout that caused this. */
class RequestTimeoutException(message: String? = null, cause: IOException? = null) : ClientException(message, cause)
