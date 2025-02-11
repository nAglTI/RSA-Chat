package com.hypergonial.chat.model.exceptions

import kotlinx.io.IOException

/** A failure has occurred at the transport layer */
class TransportException(message: String? = null, cause: IOException? = null) : ClientException(message, cause)
