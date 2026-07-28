package com.aichat.app.ui.screens.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.aichat.app.data.model.MessageEntity
import com.aichat.app.ui.components.ChatInput
import com.aichat.app.ui.screens.state.ChatScreenState
import com.aichat.app.ui.screens.utils.MessageProcessor
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
            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No chat selected.")
            }
        } else {
            ChatMessageList(
                messages = messages,
                streamingMsg = streamingMsg,
                isLoading = isLoading,
                isOcrProcessing = state.isOcrProcessing,
                listState = state.listState,
                onEditMessage = onEditMessage,
                onRegenerate = onRegenerate,
                modifier = Modifier.weight(1f)
            )

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
