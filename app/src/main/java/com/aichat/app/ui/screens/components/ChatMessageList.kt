package com.aichat.app.ui.screens.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aichat.app.data.model.MessageEntity
import com.aichat.app.ui.components.MessageBubble
import com.aichat.app.ui.components.TypingIndicator

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
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Render all standard messages from database
        items(
            items = messages,
            key = { message -> message.id } // 🚀 Unique ID for better performance
        ) { message ->
            MessageBubble(
                role = message.role,
                content = message.content
                // প্রয়োজনে onEditMessage(message) এখানে বা MessageBubble-এ পাস করতে পারেন
            )
        }

        // 2. Render currently streaming message
        if (!streamingMsg.isNullOrEmpty()) {
            item {
                MessageBubble(
                    role = "assistant",
                    content = streamingMsg
                )
            }
        }

        // 3. Render typing indicator for loading or OCR processing
        if (isLoading || isOcrProcessing) {
            item {
                TypingIndicator()
            }
        }
    }
}
