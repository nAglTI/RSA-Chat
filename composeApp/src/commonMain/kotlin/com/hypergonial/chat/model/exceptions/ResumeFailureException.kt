package com.hypergonial.chat.model.exceptions

/** An exception that is thrown when the client has failed to resume a session */
class ResumeFailureException(message: String? = null, cause: Throwable? = null) : ClientException(message, cause)
