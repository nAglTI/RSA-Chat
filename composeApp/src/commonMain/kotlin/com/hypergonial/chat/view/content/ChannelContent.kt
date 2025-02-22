package com.hypergonial.chat.view.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.LocalUsingDarkTheme
import com.hypergonial.chat.getIcon
import com.hypergonial.chat.model.Mime
import com.hypergonial.chat.trimFilename
import com.hypergonial.chat.view.components.ChannelComponent
import com.hypergonial.chat.view.composables.ChatBar
import com.hypergonial.chat.view.composables.FileDropTarget
import com.hypergonial.chat.view.composables.MessageList
import com.hypergonial.chat.view.composables.TypingIndicator

@Composable
fun ChannelContent(component: ChannelComponent) {
    val state by component.data.subscribeAsState()
    val snackbarState = remember { SnackbarHostState() }
    val isDarkMode = LocalUsingDarkTheme.current

    val canSend by
        remember(state.chatBarValue, state.pendingAttachments) {
            derivedStateOf { state.chatBarValue.text.isNotEmpty() || state.pendingAttachments.isNotEmpty() }
        }

    FileDropTarget(onFilesDropped = component::onFilesDropped) {
        Scaffold(Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarState) }) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                // Is a LazyColumn wrapped in a custom composable
                MessageList(
                    state.messageEntries,
                    Modifier.fillMaxWidth().weight(1f),
                    listState = state.listState,
                    isCruising = state.isCruising,
                )

                val dynamicMinHeight by
                    remember(state.listState) {
                        derivedStateOf {
                            if (state.listState.firstVisibleItemIndex == 0) {
                                val firstItem = state.listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                if (firstItem != null) {
                                    val visibleFraction =
                                        ((firstItem.size - state.listState.firstVisibleItemScrollOffset).toFloat() /
                                                firstItem.size)
                                            .coerceIn(0f, 1f)
                                    lerp(0.dp, 102.dp, visibleFraction)
                                } else 102.dp
                            } else 0.dp
                        }
                    }

                // The typing indicator should always fit without layout shifts
                Column(
                    Modifier.heightIn(min = dynamicMinHeight).fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Column(
                        Modifier.padding(20.dp, 0.dp, 20.dp, 20.dp)
                            .background(
                                if (isDarkMode) MaterialTheme.colorScheme.surfaceBright
                                else MaterialTheme.colorScheme.surfaceDim,
                                RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp),
                            )
                    ) {
                        ChatBarTopBar(component)

                        ChatBar(
                            state.chatBarValue,
                            Modifier.fillMaxWidth(),
                            editorKey = "MAIN_EDITOR",
                            onValueChange = component::onChatBarContentChanged,
                            onEditLastRequested = component::onEditLastMessage,
                            leadingIcon = { FileUploadIcon(component) },
                            onLeadingIconClick = component::onFileUploadDropdownOpen,
                            trailingButtonEnabled = canSend,
                            onSubmit = component::onMessageSend,
                        )
                    }
                }
            }
        }

        LaunchedEffect(state.snackbarMessage) {
            if (state.snackbarMessage.value.isNotEmpty()) {
                snackbarState.showSnackbar(state.snackbarMessage.value, withDismissAction = true)
            }
        }
    }
}

@Composable
fun FileUploadIcon(component: ChannelComponent) {
    val state by component.data.subscribeAsState()

    Box {
        Icon(
            Icons.Filled.AddCircle,
            contentDescription = "Upload Attachment",
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        )

        DropdownMenu(
            expanded = state.isFileUploadDropdownOpen,
            onDismissRequest = component::onFileUploadDropdownClose,
            shape = RoundedCornerShape(12.dp),
        ) {
            DropdownMenuItem(
                text = { Text("Upload File") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                onClick = { component.onFileAttachRequested(isMedia = false) },
                leadingIcon = { Icon(Icons.Outlined.Attachment, contentDescription = "Upload File") },
            )

            DropdownMenuItem(
                text = { Text("Upload Media") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                onClick = { component.onFileAttachRequested(isMedia = true) },
                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = "Upload Media") },
            )
        }
    }
}

@Composable
fun ChatBarTopBar(component: ChannelComponent) {
    val state by component.data.subscribeAsState()
    val mimes by
        remember(state.pendingAttachments) { derivedStateOf { state.pendingAttachments.map { Mime.fromUrl(it.name) } } }

    Column(Modifier.fillMaxWidth().padding(start = 20.dp)) {
        val typingText by remember(state.typingIndicators) {
            derivedStateOf {
                if (state.typingIndicators.size <= 3) {
                    buildAnnotatedString {
                        state.typingIndicators.values.forEachIndexed { index, user ->
                            // Make the username bold
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(user.resolvedName)
                            }
                            if (index != state.typingIndicators.size - 1) {
                                append(", ")
                            }
                        }
                        append(" ${if (state.typingIndicators.size <= 1) "is" else "are"} typing...")
                    }
                } else {
                    AnnotatedString("Multiple users are typing...")
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .animateContentSize()
                    .padding(vertical = if (state.typingIndicators.isNotEmpty()) 5.dp else 0.dp),
        ) {
            if (state.typingIndicators.isNotEmpty()) {
                val kindaGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f).compositeOver(Color.Gray)
                val lessGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f).compositeOver(Color.Gray)

                TypingIndicator(
                    initialColor = kindaGray,
                    bounceColor = MaterialTheme.colorScheme.onSurface,
                    dotSize = 5.dp,
                    bounceHeight = 5.dp,
                )
                Text(
                    typingText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp, 0.dp, 20.dp, 0.dp),
                    color = lessGray,
                    fontSize = 12.sp,
                )
            }
        }

        LazyRow(Modifier.fillMaxWidth().animateContentSize()) {
            itemsIndexed(state.pendingAttachments) { i, attachment ->
                InputChip(
                    onClick = { component.onPendingFileCancel(attachment) },
                    label = { Text(attachment.name.trimFilename()) },
                    selected = false,
                    modifier = Modifier.padding(horizontal = 5.dp),
                    avatar = {
                        Icon(mimes[i]?.getIcon() ?: Icons.Outlined.FilePresent, contentDescription = "Attachment")
                    },
                    trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Cancel") },
                )
            }
        }
    }
}
