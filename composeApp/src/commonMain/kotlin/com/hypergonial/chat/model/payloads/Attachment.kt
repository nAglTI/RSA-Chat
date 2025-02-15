package com.hypergonial.chat.model.payloads

import com.hypergonial.chat.ensureNoSlashAtEnd
import com.hypergonial.chat.model.Mime
import com.hypergonial.chat.model.settings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An attachment to a message.
 *
 * @param id The ID of the attachment.
 * @param filename The name of the file.
 * @param contentType The MIME type of the file.
 */
@Serializable
data class Attachment(val id: Int, val filename: String, @SerialName("content_type") val contentType: Mime) {

    /**
     * Constructs a URL to the attachment.
     *
     * @param message The message the attachment is attached to.
     * @return The URL to the attachment.
     */
    @Suppress("MaxLineLength")
    fun makeUrl(message: Message): String {
        return "${settings.getDevSettings().objectStoreUrl.ensureNoSlashAtEnd()}/attachments/${message.channelId}/${message.id}/$id/$filename"
    }

    companion object {
        /** A list of supported formats that can be displayed in the app. */
        val supportedEmbedFormats =
            listOf(
                Mime("image", "jpeg"),
                Mime("image", "png"),
                Mime("image", "webp"),
                Mime("image", "bmp"),
                Mime("image", "tiff"),
                // TODO: Add gif once https://github.com/coil-kt/coil/pull/2594 gets merged
            )
    }
}
