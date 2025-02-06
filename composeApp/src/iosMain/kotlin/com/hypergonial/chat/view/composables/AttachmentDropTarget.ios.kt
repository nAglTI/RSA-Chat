package com.hypergonial.chat.view.composables

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.core.PlatformFile

@Composable
actual fun AttachmentDropTarget(onFilesDropped: (List<PlatformFile>) -> Unit, content: @Composable () -> Unit) {
    content()
}
