package com.coder.app.features.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coder.app.core.model.ConversationEntity
import com.coder.app.features.chat.ui.components.drawer.*
import com.coder.app.core.common.TimeUtils
import com.coder.app.navigation.Routes

enum class DrawerMenuState { MAIN, HISTORY }

@Composable
fun DrawerContent(
    conversations: List<ConversationEntity>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onDelete: (ConversationEntity) -> Unit,
    onRename: (ConversationEntity, String) -> Unit,
    onNavigate: (String) -> Unit
) {
    // 🚀 NEW: ড্রয়ারের বর্তমান স্টেটের জন্য ভেরিয়েবল
    var currentMenu by remember { mutableStateOf(DrawerMenuState.MAIN) }

    var searchQuery by remember { mutableStateOf("") }
    var chatToRename by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var chatToDelete by remember { mutableStateOf<ConversationEntity?>(null) }

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
        if (currentMenu == DrawerMenuState.MAIN) {
            // ==========================================
            // 🚀 VIEW 1: IDE MAIN MENU
            // ==========================================
            DrawerHeader(onNewChat = onNewChat)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            NavigationDrawerItem(
                label = { Text("Agent (Chat)") },
                icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                selected = true, // Currently on Agent
                onClick = { /* Already here, just close drawer ideally */ },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            NavigationDrawerItem(
                label = { Text("File Editor") },
                icon = { Icon(Icons.Default.Code, contentDescription = null) },
                selected = false,
                onClick = { onNavigate(Routes.EDITOR) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            NavigationDrawerItem(
                label = { Text("Terminal") },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                selected = false,
                onClick = { onNavigate(Routes.TERMINAL) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            
            NavigationDrawerItem(
                label = { Text("Project History") },
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                selected = false,
                onClick = { currentMenu = DrawerMenuState.HISTORY },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
            
            DrawerSettingsItem(onSettingsClick = { onNavigate(Routes.SETTINGS) })
            
        } else {
            // ==========================================
            // 🚀 VIEW 2: PROJECT HISTORY
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMenu = DrawerMenuState.MAIN }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Project History", 
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

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
        }
    }
}
