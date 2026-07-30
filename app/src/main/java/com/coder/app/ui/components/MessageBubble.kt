package com.coder.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.coder.app.ui.components.message.AssistantMessageRow
import com.coder.app.ui.components.message.UserMessageRow

@Composable
fun MessageBubble(
    role: String,
    content: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    onEdit: () -> Unit = {},
    onRegenerate: () -> Unit = {}
) {
    val isUser = role == "user"
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(animationSpec = tween(220), initialOffsetY = { it / 6 })
    ) {
        if (isUser) {
            UserMessageRow(
                content = content,
                modifier = modifier,
                onEdit = onEdit
            )
        } else {
            AssistantMessageRow(
                content = content,
                modifier = modifier,
                isStreaming = isStreaming,
                onRegenerate = onRegenerate
            )
        }
    }
}
