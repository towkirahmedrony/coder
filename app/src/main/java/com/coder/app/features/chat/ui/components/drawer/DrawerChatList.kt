package com.coder.app.features.chat.ui.components.drawer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coder.app.core.model.ConversationEntity

@Composable
fun DrawerChatList(
    groupedChats: Map<String, List<ConversationEntity>>,
    currentId: String?,
    searchQuery: String,
    onSelect: (String) -> Unit,
    onRenameRequest: (ConversationEntity) -> Unit,
    onDeleteRequest: (ConversationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (groupedChats.isEmpty() && searchQuery.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "No Results",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No conversations found", style = MaterialTheme.typography.titleMedium)
            Text("Try another search", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = modifier.padding(top = 8.dp)) {
            groupedChats.forEach { (section, chats) ->
                item {
                    Text(
                        text = section,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                items(
                    items = chats,
                    key = { it.id }
                ) { conv ->
                    DrawerChatItem(
                        conversation = conv,
                        isSelected = conv.id == currentId,
                        onSelect = { onSelect(conv.id) },
                        onRenameRequest = { onRenameRequest(conv) },
                        onDeleteRequest = { onDeleteRequest(conv) }
                    )
                }
            }
        }
    }
}
