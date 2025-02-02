package com.hypergonial.chat.model.exceptions

import io.ktor.http.HttpStatusCode

open class ApiException(message: String? = null, cause: Throwable? = null) : ClientException(message, cause)

/** Throws the appropiate exception based on the status code */
fun getApiException(status: HttpStatusCode, message: String? = null, cause: Throwable? = null): ApiException {
    return when (status) {
        HttpStatusCode.NotFound -> NotFoundException(message, cause)
        HttpStatusCode.BadRequest -> BadRequestException(message, cause)
        HttpStatusCode.Forbidden -> ForbiddenException(message, cause)
        HttpStatusCode.Unauthorized -> UnauthorizedException(message, cause)
        else -> {
            when (status.value) {
                in 400..499 -> {
                    ClientApiException(message, cause)
                }
                in 500..599 -> {
                    ServerApiException(message, cause)
                }
                else -> {
                    ApiException(message, cause)
                }
            }
        }
    }
}
