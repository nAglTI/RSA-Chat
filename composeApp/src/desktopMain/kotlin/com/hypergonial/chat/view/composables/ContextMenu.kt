package com.hypergonial.chat.view.composables

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.SelectAll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.PlatformLocalization
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition

// Parts of this code were taken from the Compose source code and modified to fit the needs of this project.

/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** A context menu item with an icon. Intended for use with [Material3ContextMenuRepresentation].
 *
 * @param label The label of the item.
 * @param icon The icon of the item.
 * @param onClick The action to perform when the item is clicked.
 * */
class ContextMenuItemWithIcon(label: String, val icon: @Composable (() -> Unit)? = null, onClick: () -> Unit) :
    ContextMenuItem(label, onClick)

/** A Material3 context menu representation. */
class Material3ContextMenuRepresentation : ContextMenuRepresentation {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status is ContextMenuState.Status.Open) {
            var focusManager: FocusManager? by mutableStateOf(null)
            var inputModeManager: InputModeManager? by mutableStateOf(null)
            val localization = LocalLocalization.current

            Popup(
                popupPositionProvider = rememberPopupPositionProviderAtPosition(positionPx = status.rect.center),
                onDismissRequest = { state.status = ContextMenuState.Status.Closed },
                properties = PopupProperties(focusable = true),
                onPreviewKeyEvent = { false },
                onKeyEvent = {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key.nativeKeyCode) {
                            java.awt.event.KeyEvent.VK_DOWN -> {
                                inputModeManager?.requestInputMode(InputMode.Keyboard)
                                focusManager?.moveFocus(FocusDirection.Next)
                                true
                            }

                            java.awt.event.KeyEvent.VK_UP -> {
                                inputModeManager?.requestInputMode(InputMode.Keyboard)
                                focusManager?.moveFocus(FocusDirection.Previous)
                                true
                            }

                            else -> false
                        }
                    } else {
                        false
                    }
                },
            ) {
                focusManager = LocalFocusManager.current
                inputModeManager = LocalInputModeManager.current
                Column(
                    modifier =
                        Modifier
                            .shadow(16.dp, shape = RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .width(IntrinsicSize.Max)
                            .verticalScroll(rememberScrollState())
                ) {
                    items().forEach { item ->
                        MenuItemContent(
                            itemHoverColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.08f),
                            onClick = {
                                state.status = ContextMenuState.Status.Closed
                                item.onClick()
                            },
                        ) {
                            Box(Modifier.padding(vertical = 5.dp).width(20.dp).height(20.dp)) {
                                if (item is ContextMenuItemWithIcon && item.icon != null) item.icon.invoke()
                                else {
                                    Icon(getIconForBuiltIn(item.label, localization), contentDescription = item.label)
                                }
                            }

                            Text(
                                text = item.label,
                                style = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.padding(start = 10.dp, top = 5.dp, bottom = 5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemContent(itemHoverColor: Color, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    var hovered by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier.clickable(onClick = onClick)
                .onHover { hovered = it }
                .background(if (hovered) itemHoverColor else Color.Transparent)
                .fillMaxWidth()
                // Preferred min and max width used during the intrinsic measurement.
                .sizeIn(minWidth = 130.dp, maxWidth = 280.dp, minHeight = 36.dp)
                .padding(PaddingValues(horizontal = 16.dp, vertical = 0.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

private fun Modifier.onHover(onHover: (Boolean) -> Unit) =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Enter -> onHover(true)
                    PointerEventType.Exit -> onHover(false)
                }
            }
        }
    }

private fun getIconForBuiltIn(input: String, localization: PlatformLocalization): ImageVector {
    return when (input) {
        localization.cut -> Icons.Outlined.ContentCut
        localization.copy -> Icons.Outlined.ContentCopy
        localization.paste -> Icons.Outlined.ContentPaste
        localization.selectAll -> Icons.Outlined.SelectAll
        else -> Icons.Outlined.QuestionMark
    }
}
