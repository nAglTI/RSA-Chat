package com.hypergonial.chat.view.composables

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.core.PlatformFile

@Composable
actual fun FileDropTarget(onFilesDropped: (List<PlatformFile>) -> Unit, content: @Composable () -> Unit) {
    content()
}
