package com.hypergonial.chat.model.payloads

import com.hypergonial.chat.model.settings
import kotlinx.datetime.Instant
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/**
 * A common base class for a user or a member.
 *
 * @param id The ID of the user.
 * @param username The username of the user.
 * @param displayName The display name of the user.
 * @param avatarHash The hash of the user's avatar.
 * @param presence The presence of the user.
 */
@Serializable(with = PartialUserSerializer::class)
sealed interface PartialUser {
    val id: Snowflake
    val username: String
    val displayName: String?
    val avatarHash: String?
    val presence: Presence?

    val createdAt: Instant
        get() = id.createdAt

    val avatarUrl: String?
        get() =
            avatarHash?.let {
                "${settings.getApiSettings().objectStoreUrl}/users/$id/$it.${
                it.split("_").last()
            }"
            }
}

/**
 * A user of the application.
 *
 * @param id The ID of the user.
 * @param username The username of the user.
 * @param displayName The display name of the user.
 * @param avatarHash The hash of the user's avatar.
 * @param presence The presence of the user.
 */
@Serializable
open class User(
    override val id: Snowflake,
    override val username: String,
    @SerialName("display_name") override val displayName: String? = null,
    @SerialName("avatar_hash") override val avatarHash: String? = null,
    @SerialName("presence") override val presence: Presence? = null,
) : PartialUser

/**
 * A member of a guild.
 *
 * @param id The ID of the user.
 * @param username The username of the user.
 * @param displayName The display name of the user.
 * @param avatarHash The hash of the user's avatar.
 * @param nickname The nickname of the member.
 * @param guildId The ID of the guild the member is in.
 * @param joinedAt The time the member joined the guild.
 */
@Serializable(with = MemberSerializer::class)
class Member(
    id: Snowflake,
    name: String,
    displayName: String? = null,
    avatarUrl: String? = null,
    val nickname: String? = null,
    @SerialName("guild_id") val guildId: Snowflake,
    @SerialName("joined_at") val joinedAt: Instant,
) : User(id, name, displayName, avatarUrl)

@Serializable
private data class MemberPayload(
    val user: User,
    @SerialName("guild_id") val guildId: Snowflake,
    @SerialName("nickname") val nickname: String? = null,
    @SerialName("joined_at") val joinedAt: Long,
) {
    companion object {
        fun fromMember(member: Member): MemberPayload {
            return MemberPayload(member, member.guildId, member.nickname, member.joinedAt.epochSeconds)
        }

        fun toMember(memberPayload: MemberPayload): Member {
            return Member(
                memberPayload.user.id,
                memberPayload.user.username,
                memberPayload.user.displayName,
                memberPayload.user.avatarHash,
                memberPayload.nickname,
                memberPayload.guildId,
                Instant.fromEpochSeconds(memberPayload.joinedAt),
            )
        }
    }
}

// Serializer to defer serialization of Member to MemberPayload
object MemberSerializer : KSerializer<Member> {
    override val descriptor = MemberPayload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Member) {
        MemberPayload.serializer().serialize(encoder, MemberPayload.fromMember(value))
    }

    override fun deserialize(decoder: Decoder): Member {
        return MemberPayload.toMember(MemberPayload.serializer().deserialize(decoder))
    }
}

// Serializer to defer serialization of PartialUser to User or Member dynamically
object PartialUserSerializer : JsonContentPolymorphicSerializer<PartialUser>(PartialUser::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PartialUser> =
        when {
            "guild_id" in element.jsonObject -> Member.serializer()
            else -> User.serializer()
        }
}
