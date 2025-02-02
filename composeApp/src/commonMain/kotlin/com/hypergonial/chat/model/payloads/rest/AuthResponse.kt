package com.hypergonial.chat.model.payloads.rest

import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.model.payloads.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response issued by the server after a successful authentication.
 *
 * Belongs to: GET /api/v1/users/auth
 *
 * @param userId The ID of the user that was authenticated.
 * @param token The token that can be used to authenticate further requests.
 * */
@Serializable
data class AuthResponse(@SerialName("user_id") val userId: Snowflake, val token: Secret<String>)
