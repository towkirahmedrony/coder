package com.coder.app.features.chat.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.coder.app.features.chat.ui.components.ChatMainContent
import com.coder.app.features.chat.ui.components.ChatTopBar
import com.coder.app.features.chat.ui.components.DrawerContent
import com.coder.app.features.chat.ui.state.rememberChatScreenState
import com.coder.app.features.chat.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState = rememberChatScreenState()

    val conversations by viewModel.conversations.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val streamingMsg by viewModel.streamingMessage.collectAsState()
    val error by viewModel.error.collectAsState()
    val useCloudAi by viewModel.useCloudAi.collectAsState()

    val currentTitle = conversations.find { it.id == currentConvId }?.title ?: "Coder"

    LaunchedEffect(messages.size, streamingMsg) {
        if (messages.isNotEmpty() || streamingMsg != null) {
            val targetIndex = messages.size + (if (streamingMsg != null) 1 else 0)
            uiState.scrollToBottom(targetIndex)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            uiState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = uiState.drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    conversations = conversations,
                    currentId = currentConvId,
                    onSelect = {
                        viewModel.selectConversation(it)
                        uiState.closeDrawer()
                    },
                    onNewChat = {
                        viewModel.createNewChat()
                        uiState.closeDrawer()
                    },
                    onDelete = viewModel::deleteConversation,
                    onRename = viewModel::renameConversation,
                    onSettingsClick = {
                        uiState.closeDrawer()
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    title = currentTitle,
                    isCloudMode = useCloudAi,
                    onMenuClick = uiState::openDrawer
                )
            },
            snackbarHost = { SnackbarHost(uiState.snackbarHostState) }
        ) { paddingValues ->
            ChatMainContent(
                modifier = Modifier.padding(paddingValues),
                state = uiState,
                currentConvId = currentConvId,
                messages = messages,
                streamingMsg = streamingMsg,
                isLoading = isLoading,
                useCloudAi = useCloudAi,
                onEditMessage = { message ->
                    uiState.updateInputText(message.content)
                },
                onRegenerate = {
                    messages.lastOrNull { it.role == "user" }?.content?.let { lastMsg ->
                        viewModel.sendMessage(lastMsg)
                    }
                },
                onModeChange = viewModel::toggleAiMode,
                onStopGenerating = viewModel::stopGenerating,
                onSendMessage = viewModel::sendMessage
            )
        }
    }
}
