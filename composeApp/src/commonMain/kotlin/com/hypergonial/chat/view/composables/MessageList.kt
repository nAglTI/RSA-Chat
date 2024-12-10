package com.hypergonial.chat.view.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.components.EndOfMessages
import com.hypergonial.chat.view.components.LoadMoreMessagesIndicator
import com.hypergonial.chat.view.components.MessageEntry
import com.hypergonial.chat.view.components.MessageListFeature


/**
 * Composable that lazily displays a list of chat messages, requesting more as the user scrolls up.
 *
 * @param features The list of messages to display.
 * @param onMessagesLimitReach The callback that is called when the user scrolled to the top of the list
 * and we need to load more messages.
 */
@Composable
fun MessageList(
    features: List<MessageListFeature>,
    modifier: Modifier = Modifier,
    onMessagesLimitReach: (Snowflake?) -> Unit
) {
    // TODO: Possibly extract out to viewmodel
    val listState = rememberLazyListState()

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    // If the user is at the bottom of the list, keep it there.
    LaunchedEffect(features) {
        if (isAtBottom) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        modifier,
        state = listState,
        verticalArrangement = Arrangement.Bottom,
        reverseLayout = true,
    ) {
        itemsIndexed(features, key = { _, item ->  item.getKey()}) { _, item ->
            when (item) {
                is MessageEntry -> {
                    item.messages.forEach { msg ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(msg.content ?: "TODO: No content - HANDLEME")
                        }
                    }
                }

                is LoadMoreMessagesIndicator -> {
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                    ) {
                        LoadingIndicator(item, onSeen = {
                            onMessagesLimitReach(
                                features.filterIsInstance<MessageEntry>()
                                    .firstOrNull()?.messages?.first()?.id
                            )
                        })
                    }
                }

                is EndOfMessages -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("End of messages", color = Color.Red)
                    }
                }
            }
        }
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


