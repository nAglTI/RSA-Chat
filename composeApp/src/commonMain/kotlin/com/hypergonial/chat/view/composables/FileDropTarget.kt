package com.hypergonial.chat.view.composables

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.core.PlatformFile

/** Marks an area as a drop target for files.
 * Any files dragged over the area occupied by the [content] will be passed to [onFilesDropped].
 *
 * @param onFilesDropped The callback to be invoked when files are dropped on the target
 * @param content The content to be displayed in the drop target area
 */
@Composable
expect fun FileDropTarget(onFilesDropped: (List<PlatformFile>) -> Unit, content: @Composable () -> Unit)
