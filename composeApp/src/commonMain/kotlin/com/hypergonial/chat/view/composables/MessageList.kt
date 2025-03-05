package com.hypergonial.chat.view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHeader
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType

val LocalHighlights = compositionLocalOf { Highlights.Builder() }

/**
 * Composable that lazily displays a list of chat messages, requesting more as the user scrolls up.
 *
 * @param features The list of messages to display.
 * @param isCruising Whether the user currently has the bottom of the list loaded.
 */
@Composable
fun MessageList(
    features: List<MessageEntryComponent>,
    modifier: Modifier = Modifier,
    isCruising: Boolean,
    bottomSpacer: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
) {
    val isDarkTheme = LocalUsingDarkTheme.current
    val highlightsBuilder =
        remember(isDarkTheme) { Highlights.Builder().theme(SyntaxThemes.monokai(darkMode = isDarkTheme)) }

    CompositionLocalProvider(LocalHighlights provides highlightsBuilder) {
        LazyColumn(modifier, state = listState, reverseLayout = true) {
            if (!isCruising && bottomSpacer > 0.dp) {
                item(key = "BOTTOM_SPACER") { Spacer(modifier = Modifier.height(bottomSpacer)) }
            }

            itemsIndexed(features, key = { _, item -> item.getKey() }) { _, item -> Entry(item) }
        }
    }
}

/**
 * Composable that displays (potentially) a grouping of messages from the same user.
 *
 * @param component The message entry to display.
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
                    Avatar(
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

/**
 * A context menu for a message.
 *
 * @param component The message to display the context menu for.
 * @param content The composable to apply the context menu to.
 */
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
                editorKey = "EDITOR_${component.getKey()}",
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
            val textColor =
                if (state.isFailed) MaterialTheme.colorScheme.error
                else if (state.isPending) Color.Gray else MaterialTheme.colorScheme.onBackground

            val primary = MaterialTheme.colorScheme.primary

            Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Markdown(
                    state.message.content ?: "TODO: No content - HANDLEME",
                    imageTransformer = ChatImageTransformer,
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.9f),
                    components =
                        markdownComponents(
                            codeBlock = {
                                MarkdownHighlightedCodeBlock(it.content, it.node, highlights = LocalHighlights.current)
                            },
                            codeFence = {
                                MarkdownHighlightedCodeFence(it.content, it.node, highlights = LocalHighlights.current)
                            },
                            // Ignore horizontal lines
                            horizontalRule = { MarkdownText(it.content) },
                            paragraph = { ChatParagraph(it.content, it.node, style = it.typography.paragraph) },
                            heading1 = { ChatHeader(it.content, it.node, style = it.typography.h1) },
                            heading2 = { ChatHeader(it.content, it.node, style = it.typography.h2) },
                            heading3 = { ChatHeader(it.content, it.node, style = it.typography.h3) },
                            heading4 = { ChatHeader(it.content, it.node, style = it.typography.h4) },
                            heading5 = { ChatHeader(it.content, it.node, style = it.typography.h5) },
                            heading6 = { ChatHeader(it.content, it.node, style = it.typography.h6) },
                        ),
                    typography =
                        markdownTypography(
                            h1 = MaterialTheme.typography.displayLarge.copy(color = textColor),
                            h2 = MaterialTheme.typography.displayMedium.copy(color = textColor),
                            h3 = MaterialTheme.typography.displaySmall.copy(color = textColor),
                            h4 = MaterialTheme.typography.headlineMedium.copy(color = textColor),
                            h5 = MaterialTheme.typography.headlineSmall.copy(color = textColor),
                            h6 = MaterialTheme.typography.titleLarge.copy(color = textColor),
                            text =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 15.sp,
                                    color = textColor,
                                ),
                            paragraph =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 15.sp,
                                    color = textColor,
                                ),
                            quote =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Thin,
                                    fontSize = 15.sp,
                                ),
                            link =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp,
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
fun ChatParagraph(
    content: String,
    node: ASTNode,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalMarkdownTypography.current.paragraph,
) {
    val annotatorSettings = annotatorSettings()
    val styledText = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(content = content, node = node, annotatorSettings = annotatorSettings)
        pop()
    }

    MarkdownText(styledText.processChatFormatting(), modifier = modifier, style = style)
}

@Composable
fun ChatHeader(
    content: String,
    node: ASTNode,
    style: TextStyle,
    contentChildType: IElementType = MarkdownTokenTypes.ATX_CONTENT,
) = ChatText(
    modifier = Modifier.semantics {
        heading()
    },
    content = content,
    node = node,
    style = style,
    contentChildType = contentChildType,
)


@Composable
fun ChatText(
    content: String,
    node: ASTNode,
    style: TextStyle,
    modifier: Modifier = Modifier,
    contentChildType: IElementType? = null,
) {
    val annotatorSettings = annotatorSettings()
    val childNode = contentChildType?.run(node::findChildOfType) ?: node

    val styledText = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(
            content = content,
            node = childNode,
            annotatorSettings = annotatorSettings
        )
        pop()
    }

    MarkdownText(styledText.processChatFormatting(), modifier = modifier, style = style)
}

@Composable
fun AnnotatedString.processChatFormatting(): AnnotatedString {
    val mentionRegex = "<@(\\d+)>".toRegex()
    val primaryColor = MaterialTheme.colorScheme.primary

    return replaceInAnnotatedString(this, mentionRegex) { matchResult ->
        val userId = matchResult.groupValues[1].toInt()
        println("Mention with user ID: $userId")
        val userName = "username"
        buildAnnotatedString { withStyle(SpanStyle(color = primaryColor)) { append("@${userName}") } }
    }
}

fun replaceInAnnotatedString(
    original: AnnotatedString,
    pattern: Regex,
    transform: (MatchResult) -> AnnotatedString,
): AnnotatedString {
    val matches = pattern.findAll(original.text).toList()
    if (matches.isEmpty()) return original

    return buildAnnotatedString {
        var currentIndex = 0

        matches.forEach { match ->
            val rangeStart = match.range.first
            val rangeEnd = match.range.last + 1

            // Append text before the match with original styling
            if (rangeStart > currentIndex) {
                val textBefore = original.subSequence(currentIndex, rangeStart)
                append(textBefore)
            }

            // Apply the transformed text for the match
            val replacement = transform(match)
            append(replacement)

            currentIndex = rangeEnd
        }

        // Append any remaining text after the last match
        if (currentIndex < original.text.length) {
            val textAfter = original.subSequence(currentIndex, original.text.length)
            append(textAfter)
        }
    }
}

@Composable
fun DesktopOnlySelectionContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (platform.isDesktopOrWeb()) {
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

    CircularProgressIndicator(Modifier.padding(5.dp))
}
