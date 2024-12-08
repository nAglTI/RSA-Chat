package com.hypergonial.chat.model.exceptions

class AuthorizationFailedException(
    override val message: String? = null,
    override val cause: Throwable? = null)
    : Exception(message, cause)
