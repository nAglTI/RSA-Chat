package com.hypergonial.chat.view.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

enum class DrawerDirection {
    Left, Right;

    fun isLeft() = this == Left

    fun isRight() = this == Right
}

@Composable
fun AdaptiveDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    onLayoutChange: ((Boolean) -> Unit)? = null,
    drawerDirection: DrawerDirection = DrawerDirection.Left,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isSmall = remember(windowSizeClass) {
        windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
    }

    LaunchedEffect(isSmall) {
        if (!isSmall) {
            drawerState.close()
        }


        onLayoutChange?.invoke(isSmall)
    }

    val originalLayoutDir = LocalLayoutDirection.current
    val drawerLayoutDir = if (drawerDirection.isRight()) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    val shape = if (drawerDirection.isRight()) {
        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
    } else {
        RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
    }

    CompositionLocalProvider(LocalLayoutDirection provides drawerLayoutDir) {
        PermanentNavigationDrawer({
            CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDir) {
                PermanentDrawerSheet(
                    Modifier.animateContentSize().width(if (!isSmall) 200.dp else 0.dp)
                ) { Box(Modifier.padding(8.dp)) { drawerContent() } }
            }
        }, modifier) {
            ModalNavigationDrawer({
                if (isSmall) CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDir) {
                    ModalDrawerSheet(
                        drawerShape = shape,
                    ) { Box(Modifier.padding(8.dp)) { drawerContent() } }
                } else Unit
            },
                modifier,
                if (isSmall) drawerState else DrawerState(DrawerValue.Closed),
                if (isSmall) gesturesEnabled else false,
                content = {
                    CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDir) {
                        content()
                    }
                })
        }
    }


}
