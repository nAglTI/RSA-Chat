package com.hypergonial.chat.view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.core.PlatformFile
import java.awt.datatransfer.DataFlavor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
actual fun FileDropTarget(onFilesDropped: (List<PlatformFile>) -> Unit, content: @Composable () -> Unit) {
    var isActive by remember { mutableStateOf(false) }

    val dragAndDropTarget =
        remember(onFilesDropped) {
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    isActive = true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    isActive = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val files =
                        event.awtTransferable.let { transferable ->
                            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                @Suppress("UNCHECKED_CAST")
                                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>

                                files.filter { it.exists() && it.isFile }.map { PlatformFile(it) }
                            } else {
                                emptyList()
                            }
                        }

                    // Handle the dropped files
                    if (files.isNotEmpty()) {
                        onFilesDropped(files)
                    }

                    isActive = false
                    return true
                }
            }
        }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .dragAndDropTarget(shouldStartDragAndDrop = { it.isFileDrag() }, target = dragAndDropTarget)
    ) {
        content()
        // Semi-transparent box to dim the content behind the upload dialog
        AnimatedVisibility(visible = isActive, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) { /* Disable touch events */ }
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp, 0.dp, 0.dp, 0.dp))
            )
        }

        AnimatedVisibility(isActive, enter = scaleIn(), exit = scaleOut()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                        Modifier.height(120.dp)
                            .width(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        Icons.Filled.FileUpload,
                        contentDescription = "Upload files",
                        modifier = Modifier.height(48.dp).width(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text("Drop files here", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun DragAndDropEvent.isFileDrag(): Boolean {
    return this.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
}
