package com.hypergonial.chat.view.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.components.EndOfMessages
import com.hypergonial.chat.view.components.LoadMoreMessagesIndicator
import com.hypergonial.chat.view.components.MessageEntry


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
    onMessagesLimitReach: (Snowflake?, Boolean) -> Unit
) {
    // - 2 because we want to check if we were at the bottom before the last item was added
    val isAtBottom =
        (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= features.size - 2

    LaunchedEffect(features) {
        println("isAtBottom: $isAtBottom")
        println("isCruising: $isCruising")

        println("features.size: ${features.size}")
        println("listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index: ${listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index}")
        if (isAtBottom && !isCruising) {
            println("Scrolling to bottom")
            listState.requestScrollToItem(features.size - 1)
        }
    }

    LazyColumn(
        modifier,
        state = listState,
    ) {
        itemsIndexed(features, key = { _, item -> item.getKey() }) { _, item ->
            Entry(item, onEndReached = { isAtTop ->
                if (isAtTop) {
                    onMessagesLimitReach(
                        features.firstOrNull()?.messages?.firstOrNull()?.message?.id, isAtTop
                    )
                } else {
                    onMessagesLimitReach(
                        features.lastOrNull()?.messages?.lastOrNull()?.message?.id, isAtTop
                    )
                }
            })
        }
    }
}

@Composable
fun Entry(item: MessageEntry, onEndReached: (Boolean) -> Unit) {
    val endIndicator = item.endIndicator

    if (endIndicator is EndOfMessages) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("End of messages", color = Color.Red)
        }
    }

    if (endIndicator is LoadMoreMessagesIndicator && endIndicator.isAtTop) {
        LoadingIndicator(endIndicator, onSeen = { onEndReached(true) })
    }

    item.messages.forEach { entry ->
        Row(Modifier.fillMaxWidth()) {
            Text(entry.message.content ?: "TODO: No content - HANDLEME", color = if (entry.isPending) Color.Gray else Color.Unspecified)
        }
    }

    if (endIndicator is LoadMoreMessagesIndicator && !endIndicator.isAtTop) {
        LoadingIndicator(endIndicator, onSeen = { onEndReached(false) })
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


