package com.hypergonial.chat.model.payloads.rest

import com.hypergonial.chat.model.Secret
import kotlinx.serialization.Serializable

/**
 * Request to register a user.
 *
 * Belongs to: POST /api/v1/users
 *
 * @param username The username of the user.
 * @param password The password of the user.
 */
@Serializable data class UserRegisterRequest(val username: String, val password: Secret<String>)
