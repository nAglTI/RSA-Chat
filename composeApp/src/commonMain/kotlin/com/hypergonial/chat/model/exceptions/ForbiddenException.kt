package com.hypergonial.chat.model.exceptions

class ForbiddenException(message: String? = null, cause: Throwable? = null) : ClientApiException(message, cause)
