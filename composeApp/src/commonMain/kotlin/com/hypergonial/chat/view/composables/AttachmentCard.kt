package com.hypergonial.chat.view.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownloadOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.hypergonial.chat.getIcon
import com.hypergonial.chat.model.Mime
import com.hypergonial.chat.model.downloader
import com.hypergonial.chat.trimFilename

/** A card that displays an attachment with a download button
 *
 * @param filename the name of the file
 * @param mime the mime type of the file
 * @param url the url of the file
 * @param modifier the modifier for the card
 */
@Composable
fun AttachmentCard(filename: String, mime: Mime, url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Card(modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                mime.getIcon(),
                contentDescription = "File Icon for $filename",
                modifier = Modifier.padding(15.dp).height(48.dp).width(48.dp),
            )
            HyperText(filename.trimFilename(), mapOf(filename.trimFilename() to url))
            IconButton(
                onClick = { downloader.downloadFile(url, uriHandler) },
                Modifier.pointerHoverIcon(PointerIcon.Hand),
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Download $filename")
            }
        }
    }
}

/** A card that displays a failed attachment with a disabled download button
 *
 * @param filename the name of the file
 * @param modifier the modifier for the card
 */
@Composable
fun FailedAttachmentCard(filename: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Error,
                contentDescription = "Upload Failure",
                modifier = Modifier.padding(15.dp).height(48.dp).width(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(filename.trimFilename(), color = MaterialTheme.colorScheme.error)

            IconButton(
                onClick = {},
                enabled = false,
            ) {
                Icon(Icons.Filled.FileDownloadOff, contentDescription = "Download $filename")
            }

        }
    }
}

