package com.hypergonial.chat.view.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupPositionProvider
import com.hypergonial.chat.platform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopOnlyTooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (platform.isDesktopOrWeb()) {
        TooltipBox(
            positionProvider = positionProvider,
            tooltip = tooltip,
            state = state,
            modifier = modifier,
            focusable = focusable,
            enableUserInput = enableUserInput,
            content = content,
        )
    } else {
        content()
    }
}
