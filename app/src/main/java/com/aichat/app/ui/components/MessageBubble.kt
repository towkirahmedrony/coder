package com.aichat.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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

private val UserBubbleShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
private val BotBubbleShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
private const val CHAR_LIMIT = 800

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    role: String,
    content: String,
    onEdit: () -> Unit = {},
    onRegenerate: () -> Unit = {}
) {
    val isUser = role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isUser) UserBubbleShape else BotBubbleShape
    
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    val isLongText = isUser && content.length > CHAR_LIMIT
    var isExpanded by remember { mutableStateOf(false) }

    val displayContent = remember(content, isExpanded, isUser) {
        if (isLongText && !isExpanded) {
            val truncated = content.take(CHAR_LIMIT)
            val codeBlockCount = truncated.split("```").size - 1
            if (codeBlockCount % 2 != 0) "$truncated\n```\n..." else "$truncated..."
        } else {
            content
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.weight(0.9f, fill = false), 
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleColor, 
                shape = shape, 
                shadowElevation = 1.dp, 
                modifier = Modifier.animateContentSize()
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .run {
                                if (isUser) {
                                    combinedClickable(
                                        onClick = { focusManager.clearFocus() },
                                        onLongClick = { showMenu = true }
                                    )
                                } else {
                                    this
                                }
                            }
                            .padding(14.dp)
                    ) {
                        ParsedMessageContent(displayContent, textColor, clipboardManager)

                        if (isLongText) {
                            Text(
                                text = if (isExpanded) "Show less" else "Read more",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { isExpanded = !isExpanded }
                            )
                        }
                    }
                    
                    if (isUser) {
                        DropdownMenu(
                            expanded = showMenu, 
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy") }, 
                                onClick = { 
                                    clipboardManager.setText(AnnotatedString(content)) 
                                    showMenu = false 
                                    focusManager.clearFocus() 
                                }, 
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit") }, 
                                onClick = { 
                                    onEdit() 
                                    showMenu = false 
                                    focusManager.clearFocus() 
                                }, 
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                            )
                        }
                    }
                }
            }

            if (!isUser) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp), 
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = { 
                            clipboardManager.setText(AnnotatedString(content)) 
                            focusManager.clearFocus() 
                        }, 
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy, 
                            contentDescription = "Copy", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { 
                            onRegenerate() 
                            focusManager.clearFocus() 
                        }, 
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh, 
                            contentDescription = "Regenerate", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
