package com.hypergonial.chat.model.exceptions

class BadRequestException(message: String? = null, cause: Throwable? = null) : ClientApiException(message, cause)
