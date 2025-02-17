package com.hypergonial.chat.view.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.hypergonial.chat.view.editorFocusInhibitor

enum class DrawerDirection {
    Left,
    Right;

    fun isLeft() = this == Left

    fun isRight() = this == Right
}

/**
 * A drawer that adapts to the window size
 *
 * @param drawerContent The content of the drawer
 * @param modifier The modifier to be applied to the drawer
 * @param drawerState The state of the drawer
 * @param gesturesEnabled Whether touch gestures are enabled
 * @param onLayoutChange The callback to call when the layout changes
 * @param windowInsets The window insets to be applied to the drawer content
 * @param drawerDirection Controls which side the drawer is on
 * @param content The content the drawer is applied to
 */
@Composable
fun AdaptiveDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    onLayoutChange: ((Boolean) -> Unit)? = null,
    windowInsets: WindowInsets = WindowInsets(0),
    drawerDirection: DrawerDirection = DrawerDirection.Left,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) { windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }

    DisposableEffect(isSmall, drawerState.targetValue) {
        if (!isSmall) {
            editorFocusInhibitor.release("MODAL_DRAWER")
        } else if (drawerState.targetValue == DrawerValue.Open) {
            editorFocusInhibitor.acquire("MODAL_DRAWER")
        } else if (drawerState.targetValue == DrawerValue.Closed) {
            editorFocusInhibitor.release("MODAL_DRAWER")
        }

        onDispose { editorFocusInhibitor.release("MODAL_DRAWER") }
    }

    LaunchedEffect(isSmall) {
        if (!isSmall) {
            drawerState.close()
        }

        onLayoutChange?.invoke(isSmall)
    }

    val originalLayoutDir = LocalLayoutDirection.current
    val drawerLayoutDir =
        if (drawerDirection.isRight()) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    val shape =
        if (drawerDirection.isRight()) {
            RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
        } else {
            RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
        }

    CompositionLocalProvider(LocalLayoutDirection provides drawerLayoutDir) {
        PermanentNavigationDrawer(
            {
                CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDir) {
                    PermanentDrawerSheet(
                        Modifier.animateContentSize().width(if (!isSmall) 300.dp else 0.dp),
                        windowInsets = windowInsets,
                    ) {
                        drawerContent()
                    }
                }
            },
            modifier,
        ) {
            ModalNavigationDrawer(
                drawerContent = {
                    if (isSmall) {
                        CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDir) {
                            DrawerDefaults.windowInsets
                            ModalDrawerSheet(drawerShape = shape, windowInsets = windowInsets) { drawerContent() }
                        }
                    }
                },
                modifier,
                if (isSmall) drawerState else DrawerState(DrawerValue.Closed),
                if (isSmall) gesturesEnabled else false,
                content = { CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDir) { content() } },
            )
        }
    }
}
