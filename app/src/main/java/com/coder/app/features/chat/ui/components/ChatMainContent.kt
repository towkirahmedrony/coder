package com.coder.app.features.chat.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.app.core.model.MessageEntity
import com.coder.app.features.chat.ui.components.ChatInput
import com.coder.app.features.chat.ui.state.ChatScreenState
import com.coder.app.features.chat.data.MessageProcessor
import kotlinx.coroutines.launch

@Composable
fun ChatMainContent(
    modifier: Modifier = Modifier,
    state: ChatScreenState,
    currentConvId: String?,
    messages: List<MessageEntity>,
    streamingMsg: String?,
    isLoading: Boolean,
    useCloudAi: Boolean,
    onEditMessage: (MessageEntity) -> Unit,
    onRegenerate: () -> Unit,
    onModeChange: (Boolean) -> Unit,
    onStopGenerating: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { state.updateSelectedUri(it) }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { state.updateSelectedUri(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        if (currentConvId == null) {
            WelcomeEmptyState(modifier = Modifier.weight(1f).fillMaxSize())
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            ) {
                ChatMessageList(
                    messages = messages,
                    streamingMsg = streamingMsg,
                    isLoading = isLoading,
                    isOcrProcessing = state.isOcrProcessing,
                    listState = state.listState,
                    onEditMessage = onEditMessage,
                    onRegenerate = onRegenerate,
                    modifier = Modifier.fillMaxSize()
                )

                val showScrollToBottom by remember {
                    derivedStateOf {
                        val layoutInfo = state.listState.layoutInfo
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        layoutInfo.totalItemsCount > 1 && lastVisible < layoutInfo.totalItemsCount - 2
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            state.coroutineScope.launch {
                                val target = (state.listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                state.listState.animateScrollToItem(target)
                            }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to latest")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            AiModeSwitcher(
                useCloudAi = useCloudAi,
                onModeChange = onModeChange
            )

            ChatInput(
                text = state.inputText,
                onTextChange = state::updateInputText,
                isLoading = isLoading || state.isOcrProcessing,
                selectedUri = state.selectedUri,
                onImageAttachClick = {
                    imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onFileAttachClick = { fileLauncher.launch(arrayOf("*/*")) },
                onRemoveAttachment = { state.updateSelectedUri(null) },
                onStopGenerating = onStopGenerating,
                onSendMessage = {
                    val textToSend = state.inputText
                    val uriToSend = state.selectedUri

                    state.updateInputText("")
                    state.updateSelectedUri(null)
                    focusManager.clearFocus()

                    state.coroutineScope.launch {
                        MessageProcessor.processAndSend(
                            context = state.context,
                            uri = uriToSend,
                            text = textToSend,
                            onProcessingStateChange = state::updateOcrProcessing,
                            onSend = onSendMessage
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomeEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Select a conversation from the menu, or start a new chat to begin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
