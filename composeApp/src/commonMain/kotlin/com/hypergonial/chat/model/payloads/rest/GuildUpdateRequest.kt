package com.hypergonial.chat.model.payloads.rest

import com.hypergonial.chat.toDataUrl
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuildUpdateRequest(
    val name: OmittedOr<String?> = Omitted,
    val avatar: OmittedOr<String?> = Omitted,
    @SerialName("owner_id") val ownerId: OmittedOr<String?> = Omitted,
) {
    class Builder {
        private var _name: OmittedOr<String?> = Omitted
        private var _avatar: OmittedOr<PlatformFile?> = Omitted
        private var _ownerId: OmittedOr<String?> = Omitted

        var name: String? by ::_name.delegate()
        var avatar: PlatformFile? by ::_avatar.delegate()
        var ownerId: String? by ::_ownerId.delegate()

        suspend fun build() = GuildUpdateRequest(_name, _avatar.map { it.toDataUrl() }, _ownerId)
    }
}
