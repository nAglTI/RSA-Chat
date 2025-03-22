package com.hypergonial.chat.view.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hypergonial.chat.model.downloader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetViewerDialog(isActive: Boolean = false, url: String? = null, onClose: () -> Unit) {
    EditorFocusInhibitor("ASSET_VIEWER", isActive)

    if (!isActive || url == null) {
        return
    }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }
    val imagePadding by animateDpAsState(if (isSmall) 20.dp else 40.dp)
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current

    // Clear focus when the overlay activates (to prevent the IME staying open)
    LaunchedEffect(isActive) {
        if (isActive) {
            focusManager.clearFocus()
        }
    }

    AnimatedDialog(
        onDismissRequest = onClose,
        properties = AnimatedDialogProperties(dismissOnClickOutside = true),
        enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut() + scaleOut(spring(stiffness = Spring.StiffnessMedium)),
    ) {
        Box(modifier = Modifier.padding(horizontal = imagePadding, vertical = imagePadding * 2)) {
            ZoomableAsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
                contentDescription = "Asset being observed",
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(null, null) { /* Eat click events */ },
                contentScale = ContentScale.Fit,
                clipToBounds = false,
            )

            IconButton(
                onClick = { uriHandler.openUri(url) },
                modifier = Modifier.align(Alignment.BottomStart).offset(y = 50.dp).pointerHoverIcon(PointerIcon.Hand),
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text("Open in Browser")
                        }
                    },
                    state = rememberTooltipState(isPersistent = true),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open in browser")
                }
            }

            IconButton(
                onClick = { downloader.downloadFile(url, uriHandler) },
                modifier = Modifier.align(Alignment.BottomEnd).offset(y = 50.dp).pointerHoverIcon(PointerIcon.Hand),
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text("Download")
                        }
                    },
                    state = rememberTooltipState(isPersistent = true),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = "Download")
                }
            }
        }
    }
}
