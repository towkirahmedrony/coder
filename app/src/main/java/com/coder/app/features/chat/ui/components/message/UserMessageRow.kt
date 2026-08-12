package com.coder.app.features.chat.ui.components.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.app.features.chat.ui.components.ParsedMessageContent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserMessageRow(
    content: String,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    var showMenu by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val isLongText = content.length > MessageConstants.CHAR_LIMIT

    val displayContent = remember(content, isExpanded) {
        if (isLongText && !isExpanded) {
            val truncated = content.take(MessageConstants.CHAR_LIMIT)
            val codeBlockCount = truncated.split("```").size - 1
            if (codeBlockCount % 2 != 0) "$truncated\n```\n..." else "$truncated..."
        } else {
            content
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(0.82f, fill = false),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MessageConstants.UserBubbleShape,
                modifier = Modifier.animateContentSize()
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { focusManager.clearFocus() },
                                onLongClick = { showMenu = true }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Fixed: Using textColor instead of color to match exact signature
                        ParsedMessageContent(
                            content = displayContent,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            clipboardManager = clipboardManager
                        )

                        if (isLongText) {
                            Text(
                                text = if (isExpanded) "Show less" else "Read more",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { isExpanded = !isExpanded }
                            )
                        }
                    }

                    UserMessageDropdownMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(content))
                            showMenu = false
                            focusManager.clearFocus()
                        },
                        onEdit = {
                            onEdit()
                            showMenu = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))
        MessageAvatar(isUser = true)
    }
}

@Composable
private fun UserMessageDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = onCopy,
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
        )
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = onEdit,
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
        )
    }
}
