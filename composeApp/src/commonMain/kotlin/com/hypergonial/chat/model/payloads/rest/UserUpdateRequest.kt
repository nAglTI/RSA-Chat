package com.hypergonial.chat.model.payloads.rest

import com.hypergonial.chat.toDataUrl
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateRequest(
    val username: OmittedOr<String?> = Omitted,
    @SerialName("display_name") val displayName: OmittedOr<String?> = Omitted,
    val avatar: OmittedOr<String?> = Omitted,
) {
    class Builder {
        private var _username: OmittedOr<String?> = Omitted
        private var _displayName: OmittedOr<String?> = Omitted
        private var _avatar: OmittedOr<PlatformFile?> = Omitted

        var username: String? by ::_username.delegate()
        var displayName: String? by ::_displayName.delegate()
        var avatar: PlatformFile? by ::_avatar.delegate()

        suspend fun build() = UserUpdateRequest(_username, _displayName, _avatar.map { it.toDataUrl() })
    }
}
