package com.hypergonial.chat.model.exceptions

class RatelimitedException(message: String? = null, cause: Throwable? = null) : ClientApiException(message, cause)
