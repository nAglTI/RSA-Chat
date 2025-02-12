package com.hypergonial.chat.view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.arkivanov.essenty.backhandler.BackHandler
import com.hypergonial.chat.model.downloader

/**
 * An overlay that shows a network asset to the user.
 *
 * @param isActive Whether the overlay is active or not
 * @param url The URL of the asset to display
 * @param onClose The callback to be invoked when the user closes the overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetViewerOverlay(
    isActive: Boolean = false,
    url: String? = null,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (url == null) {
        content()
        return
    }

    // Handle backs to close the overlay
    BackHandler(isEnabled = isActive, onBack = onClose)

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }
    val imagePadding by animateDpAsState(if (isSmall) 10.dp else 40.dp)
    val uriHandler = LocalUriHandler.current

    Box(Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(isActive, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        }

        AnimatedVisibility(
            isActive,
            enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)),
            exit = scaleOut(spring(stiffness = Spring.StiffnessMedium)),
        ) {
            Box(Modifier.fillMaxSize().clickable(null, indication = null) { onClose() }, Alignment.Center) {
                Box(modifier = Modifier.padding(horizontal = imagePadding, vertical = imagePadding * 2)) {
                    ZoomableAsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
                        contentDescription = "Asset being observed",
                        modifier =
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable(null, null) { /* Eat click events */ },
                        contentScale = ContentScale.Fit,
                        clipToBounds = false,
                    )

                    IconButton(
                        onClick = { uriHandler.openUri(url) },
                        modifier =
                            Modifier.align(Alignment.BottomStart).offset(y = 50.dp).pointerHoverIcon(PointerIcon.Hand),
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
                        onClick = { downloader.downloadFile(url!!, uriHandler) },
                        modifier =
                            Modifier.align(Alignment.BottomEnd).offset(y = 50.dp).pointerHoverIcon(PointerIcon.Hand),
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
    }
}
