package com.coder.app.features.chat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.coder.app.core.model.ConversationEntity
import com.coder.app.features.chat.ui.components.drawer.*
import com.coder.app.core.common.TimeUtils

@Composable
fun DrawerContent(
    conversations: List<ConversationEntity>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onDelete: (ConversationEntity) -> Unit,
    onRename: (ConversationEntity, String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var chatToRename by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var chatToDelete by remember { mutableStateOf<ConversationEntity?>(null) }

    // Performance Optimization: Memoize filtering and grouping calculations
    val filteredChats = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations else {
            conversations.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val groupedChats = remember(filteredChats) {
        filteredChats.groupBy { TimeUtils.getSectionTitle(it.updatedAt) }
    }

    // Dialog Handlers
    chatToRename?.let { conversation ->
        RenameChatDialog(
            conversation = conversation,
            renameText = renameText,
            onRenameTextChange = { renameText = it },
            onConfirm = {
                if (renameText.isNotBlank()) onRename(conversation, renameText)
                chatToRename = null
            },
            onDismiss = { chatToRename = null }
        )
    }

    chatToDelete?.let { conversation ->
        DeleteChatDialog(
            conversation = conversation,
            onConfirm = {
                onDelete(conversation)
                chatToDelete = null
            },
            onDismiss = { chatToDelete = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DrawerHeader(onNewChat = onNewChat)

        DrawerSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        DrawerChatList(
            groupedChats = groupedChats,
            currentId = currentId,
            searchQuery = searchQuery,
            onSelect = onSelect,
            onRenameRequest = { conv ->
                chatToRename = conv
                renameText = conv.title
            },
            onDeleteRequest = { conv ->
                chatToDelete = conv
            },
            modifier = Modifier.weight(1f)
        )

        DrawerSettingsItem(onSettingsClick = onSettingsClick)
    }
}
