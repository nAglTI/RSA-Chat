package com.hypergonial.chat.view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.hypergonial.chat.LocalUsingDarkTheme
import com.hypergonial.chat.altClickable
import com.hypergonial.chat.model.payloads.Attachment
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.platform
import com.hypergonial.chat.toHumanReadable
import com.hypergonial.chat.view.ChatImageTransformer
import com.hypergonial.chat.view.components.subcomponents.EndOfMessages
import com.hypergonial.chat.view.components.subcomponents.LoadMoreMessagesIndicator
import com.hypergonial.chat.view.components.subcomponents.MessageComponent
import com.hypergonial.chat.view.components.subcomponents.MessageEntryComponent
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

val LocalHighlights = compositionLocalOf { Highlights.Builder() }

/**
 * Composable that lazily displays a list of chat messages, requesting more as the user scrolls up.
 *
 * @param features The list of messages to display.
 * @param isCruising Whether the user currently has the bottom of the list loaded.
 * @param onMessagesLimitReach The callback that is called when the user scrolled to the top of the list and we need to
 *   load more messages.
 */
@Composable
@Suppress("UnusedParameter")
fun MessageList(
    features: List<MessageEntryComponent>,
    modifier: Modifier = Modifier,
    isCruising: Boolean,
    listState: LazyListState = rememberLazyListState(),
) {
    val isDarkTheme = LocalUsingDarkTheme.current
    val highlightsBuilder =
        remember(isDarkTheme) { Highlights.Builder().theme(SyntaxThemes.monokai(darkMode = isDarkTheme)) }

    CompositionLocalProvider(LocalHighlights provides highlightsBuilder) {
        LazyColumn(modifier, state = listState, reverseLayout = true) {
            itemsIndexed(features, key = { _, item -> item.getKey() }) { _, item -> Entry(item) }
        }
    }
}

/**
 * Composable that displays (potentially) a grouping of messages from the same user.
 *
 * @param component The message entry to display.
 * @param onEndReached The callback that is called when the user scrolled to the end of the list and we need to load
 *   more messages. This is only called if the entry contains a LoadMoreMessagesIndicator. The first parameter is the ID
 *   of the message to fetch more messages before or after, and the second parameter is whether the message is at the
 *   top of the list or not.
 */
@Composable
fun Entry(component: MessageEntryComponent) {
    val state by component.data.subscribeAsState()
    LocalUsingDarkTheme.current

    val topEndIndicator = state.topEndIndicator
    val bottomEndIndicator = state.bottomEndIndicator
    val firstItem = state.messages.firstOrNull()

    Column {
        if (state.topEndIndicator is EndOfMessages) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("End of messages", color = Color.Red)
            }
        }

        if (topEndIndicator is LoadMoreMessagesIndicator) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                LoadingIndicator(topEndIndicator, onSeen = { component.onEndReached(isAtTop = true) })
            }
        }

        if (firstItem != null) {
            Row(Modifier.padding(vertical = 10.dp)) {
                // Avatar
                Column {
                    UserAvatar(
                        firstItem.data.value.message.author.avatarUrl,
                        firstItem.data.value.message.author.resolvedName,
                    )
                }

                Column {
                    MessageWithHeader(firstItem)

                    state.messages.drop(1).forEach { msgcomp -> MessageWithoutHeader(msgcomp) }
                }
            }
        }

        if (bottomEndIndicator != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                LoadingIndicator(bottomEndIndicator, onSeen = { component.onEndReached(isAtTop = false) })
            }
        }
    }
}

@Composable
fun MessageContextMenu(component: MessageComponent, content: @Composable () -> Unit) {
    val state by component.data.subscribeAsState()
    val clipboardManager = LocalClipboardManager.current

    AltActionMenu(
        isActive = state.isAltMenuOpen,
        onDismissRequest = { component.onAltMenuStateChange(isOpen = false) },
        altActions = {
            item(
                "Copy",
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy") },
                showOnDesktop = false,
            ) {
                clipboardManager.setText(AnnotatedString(state.message.content ?: ""))
            }

            if (state.canEdit) {
                item("Edit", leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "Edit") }) {
                    // Workaround for editor grabbing focus and not allowing the menu to dismiss properly
                    component.onAltMenuStateChange(isOpen = false)
                    component.onEditStart()
                }
            }

            if (state.canDelete) {
                item("Delete", leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }) {
                    component.onDeleteRequested()
                }
            }

            if (settings.getDevSettings().isInDeveloperMode && !state.isPending && !state.isFailed) {
                item(
                    "Copy Message ID",
                    leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = "Developer Mode") },
                ) {
                    clipboardManager.setText(AnnotatedString(state.message.id.toString()))
                }
            }
        },
    ) {
        content()
    }
}

/** A message with a username and timestamp attached to it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageWithHeader(component: MessageComponent) {
    val state by component.data.subscribeAsState()

    MessageContextMenu(component) {
        DesktopOnlySelectionContainer {
            Column(Modifier.combinedClickable(onDoubleClick = { component.onEditStart() }) {}) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        state.message.author.displayName ?: state.message.author.username,
                        Modifier.padding(end = 8.dp),
                    )
                    Text(state.createdAt.toHumanReadable(), fontSize = 10.sp, color = Color.Gray)
                }
                MessageContent(component, Modifier.padding(end = 40.dp))
            }
        }
    }
}

/** A message without a username and timestamp attached to it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageWithoutHeader(component: MessageComponent) {
    MessageContextMenu(component) {
        DesktopOnlySelectionContainer {
            Row(Modifier.fillMaxWidth().combinedClickable(onDoubleClick = { component.onEditStart() }) {}) {
                MessageContent(component, Modifier.padding(end = 40.dp))
            }
        }
    }
}

/** The content of a message in markdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContent(component: MessageComponent, modifier: Modifier = Modifier) {
    val state by component.data.subscribeAsState()

    BackHandler(isEnabled = state.isBeingEdited, onBack = { component.onEditCancel() })

    Column(Modifier.altClickable { component.onAltMenuStateChange(isOpen = true) }) {
        if (state.isBeingEdited) {
            ChatBar(
                value = state.editorState,
                onValueChange = { component.onEditorStateChanged(it) },
                onSubmit = { component.onEditFinish() },
                onFocusLoss = { component.onEditCancel() },
                shouldGrabFocus = true,
                modifier = modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        Icons.Filled.Done,
                        contentDescription = "Done",
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    )
                },
            )
        } else {
            Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Markdown(
                    state.message.content ?: "TODO: No content - HANDLEME",
                    colors =
                        markdownColor(
                            text =
                                if (state.isFailed) MaterialTheme.colorScheme.error
                                else if (state.isPending) Color.Gray else MaterialTheme.colorScheme.onBackground,
                            linkText = MaterialTheme.colorScheme.primary,
                        ),
                    imageTransformer = ChatImageTransformer,
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.9f),
                    components =
                        markdownComponents(
                            codeBlock = { MarkdownHighlightedCodeBlock(it.content, it.node, LocalHighlights.current) },
                            codeFence = { MarkdownHighlightedCodeFence(it.content, it.node, LocalHighlights.current) },
                            // Ignore horizontal lines
                            horizontalRule = { MarkdownText(it.content) },
                        ),
                    typography =
                        markdownTypography(
                            text = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                            paragraph = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                            quote =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Thin,
                                ),
                            link =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    textDecoration = TextDecoration.Underline,
                                ),
                        ),
                )

                AnimatedVisibility(visible = state.message.isEdited) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ) {
                                Text("Edited")
                            }
                        },
                        state = rememberTooltipState(isPersistent = true),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edited", tint = Color.Gray)
                    }
                }
            }
        }

        if (state.hasUploadingAttachments) {
            UploadStateCard(state.uploadProgress.toFloat())
        } else if (state.message.attachments.isNotEmpty()) {
            if (state.isFailed) {
                state.message.attachments.forEach { attachment -> FailedAttachmentCard(attachment.filename) }
                return@Column
            }

            state.message.attachments.forEach { attachment ->
                val url = attachment.makeUrl(state.message)

                if (attachment.contentType !in Attachment.supportedEmbedFormats) {
                    AttachmentCard(
                        attachment.filename,
                        attachment.contentType,
                        url,
                        modifier = Modifier.padding(top = 8.dp, end = 20.dp),
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
                        contentDescription = attachment.filename,
                        modifier =
                            Modifier.padding(top = 8.dp, end = 20.dp)
                                .widthIn(Dp.Unspecified, 500.dp)
                                .heightIn(Dp.Unspecified, 500.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(null, indication = null) { component.onAttachmentClicked(attachment.id) }
                                .pointerHoverIcon(PointerIcon.Hand),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopOnlySelectionContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (platform.isDesktop()) {
        SelectionContainer(modifier) { content() }
    } else {
        content()
    }
}

@Composable
fun LoadingIndicator(item: LoadMoreMessagesIndicator, onSeen: () -> Unit) {
    LaunchedEffect(Unit) {
        if (!item.wasSeen) {
            item.wasSeen = true
            onSeen()
        }
    }

    CircularProgressIndicator()
}
