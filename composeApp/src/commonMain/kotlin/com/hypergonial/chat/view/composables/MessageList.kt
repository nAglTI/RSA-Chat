package com.hypergonial.chat.view.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.composeapp.generated.resources.Res
import chat.composeapp.generated.resources.avatar_placeholder
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hypergonial.chat.LocalUsingDarkTheme
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.components.EndOfMessages
import com.hypergonial.chat.view.components.LoadMoreMessagesIndicator
import com.hypergonial.chat.view.components.MessageEntry
import com.hypergonial.chat.view.components.MessageUIState
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource


/**
 * Composable that lazily displays a list of chat messages, requesting more as the user scrolls up.
 *
 * @param features The list of messages to display.
 * @param isCruising Whether the user currently has the bottom of the list loaded.
 * @param onMessagesLimitReach The callback that is called when the user scrolled to the top of the list
 * and we need to load more messages.
 */
@Composable
fun MessageList(
    features: List<MessageEntry>,
    isCruising: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onMessagesLimitReach: (Snowflake?, Boolean) -> Unit,
    onEditRequested: (Snowflake) -> Unit = {},
    onEditConfirmed: () -> Unit = {},
    onEditCancelled: () -> Unit = {}
) {
    LazyColumn(
        modifier,
        state = listState,
    ) {
        itemsIndexed(features, key = { _, item -> item.getKey() }) { _, item ->
            Entry(item, onEndReached = { isAtTop ->
                if (isAtTop) {
                    onMessagesLimitReach(
                        features.firstOrNull()?.components?.firstOrNull()?.message?.id, isAtTop
                    )
                } else {
                    onMessagesLimitReach(
                        features.lastOrNull()?.components?.lastOrNull()?.message?.id, isAtTop
                    )
                }
            }, onEditRequested, onEditConfirmed, onEditCancelled)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Entry(
    item: MessageEntry,
    onEndReached: (Boolean) -> Unit,
    onEditRequested: (Snowflake) -> Unit = {},
    onEditConfirmed: () -> Unit = {},
    onEditCancelled: () -> Unit = {}
) {
    val endIndicator = item.endIndicator
    val isDarkTheme = LocalUsingDarkTheme.current
    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.monokai(darkMode = isDarkTheme))
    }


    if (endIndicator is EndOfMessages) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("End of messages", color = Color.Red)
        }
    }

    if (endIndicator is LoadMoreMessagesIndicator && endIndicator.isAtTop) {
        LoadingIndicator(endIndicator, onSeen = { onEndReached(true) })
    }

    val firstItem = item.components.firstOrNull()

    if (firstItem != null) {
        Row(Modifier.padding(vertical = 10.dp)) {
            Column {
                val imageModifier =
                    Modifier.padding(vertical = 6.dp, horizontal = 14.dp).clip(CircleShape)
                        .height(40.dp).width(40.dp)

                if (firstItem.message.author.avatarUrl == null) {
                    Image(
                        painter = painterResource(Res.drawable.avatar_placeholder),
                        contentDescription = "User avatar",
                        modifier = imageModifier,
                        colorFilter = if (isDarkTheme) ColorFilter.tint(Color.White) else null
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(firstItem.message.author.avatarUrl).crossfade(true).build(),
                        contentDescription = "User avatar",
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier,
                    )
                }
            }

            Column {
                Row {
                    Text(firstItem.message.author.displayName, Modifier.padding(end = 8.dp))
                    Text(
                        firstItem.message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            .toString(), fontSize = 10.sp, color = Color.Gray
                    )
                }

                item.components.forEach { entry ->
                    Row(Modifier.fillMaxWidth()
                        .combinedClickable(onDoubleClick = { onEditRequested(entry.message.id) }) { },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MessageContent(entry, highlightsBuilder)
                    }
                }
            }
        }
    }

    if (endIndicator is LoadMoreMessagesIndicator && !endIndicator.isAtTop) {
        LoadingIndicator(endIndicator, onSeen = { onEndReached(false) })
    }


}

@Composable
fun MessageContent(
    state: MessageUIState, highlights: Highlights.Builder, modifier: Modifier = Modifier
) {
    if (state.isBeingEdited) {
        ChatBar(
            value = state.editingContent,
            onTextChange = { state.editingContent = it },
            onSend = { state.isBeingEdited = false },
            modifier = modifier
        )
    }

    SelectionContainer {
        Markdown(
            state.message.content ?: "TODO: No content - HANDLEME",
            colors = markdownColor(
                text = if (state.isPending) Color.Gray else MaterialTheme.colorScheme.onBackground
            ),
            imageTransformer = Coil3ImageTransformerImpl,
            components = markdownComponents(
                codeBlock = { MarkdownHighlightedCodeBlock(it.content, it.node, highlights) },
                codeFence = { MarkdownHighlightedCodeBlock(it.content, it.node, highlights) },
            ),
            modifier = modifier,
            typography = markdownTypography(
                text = MaterialTheme.typography.bodyMedium,
                paragraph = MaterialTheme.typography.bodyMedium
            ),
        )
    }

}


@Composable
fun LoadingIndicator(item: LoadMoreMessagesIndicator, onSeen: () -> Unit) {
    LaunchedEffect(Unit) {
        if (!item.wasSeen) {
            println("Loading more messages")
            item.wasSeen = true
            onSeen()
        }
    }

    CircularProgressIndicator()
}


