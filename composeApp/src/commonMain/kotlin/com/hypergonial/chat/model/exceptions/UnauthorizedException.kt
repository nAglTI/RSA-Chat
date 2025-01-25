package com.hypergonial.chat.model.exceptions

class UnauthorizedException(message: String? = null, cause: Throwable? = null) : ClientApiException(message, cause)
