package com.coder.app.features.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coder.app.core.model.MessageEntity
import com.coder.app.features.chat.ui.components.MessageBubble
import com.coder.app.features.chat.ui.components.TypingIndicator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageList(
    messages: List<MessageEntity>,
    streamingMsg: String? = null,
    isLoading: Boolean = false,
    isOcrProcessing: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    onEditMessage: (MessageEntity) -> Unit = {},
    onRegenerate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
    ) {
        items(
            items = messages,
            key = { message -> message.id }
        ) { message ->
            MessageBubble(
                role = message.role,
                content = message.content,
                modifier = Modifier.animateItemPlacement(),
                onEdit = { onEditMessage(message) }
            )
        }

        if (!streamingMsg.isNullOrEmpty()) {
            item(key = "streaming_message") {
                MessageBubble(
                    role = "assistant",
                    content = streamingMsg,
                    isStreaming = true,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }

        if (isLoading || isOcrProcessing) {
            item(key = "typing_indicator") {
                // TypingIndicator-এর সরাসরি modifier না থাকায় একে Box-এ র‍্যাপ করা হয়েছে
                Box(modifier = Modifier.animateItemPlacement()) {
                    TypingIndicator()
                }
            }
        }
    }
}
