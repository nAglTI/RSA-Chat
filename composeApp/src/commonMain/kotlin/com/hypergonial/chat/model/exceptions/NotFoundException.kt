package com.hypergonial.chat.model.exceptions

class NotFoundException(message: String? = null, cause: Throwable? = null) : ClientApiException(message, cause)
