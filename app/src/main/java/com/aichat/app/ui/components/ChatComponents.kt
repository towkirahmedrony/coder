package com.aichat.app.ui.components

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
    val shape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    val charLimit = 800
    val isLongText = isUser && content.length > charLimit
    var isExpanded by remember { mutableStateOf(false) }

    val displayContent = remember(content, isExpanded, isUser) {
        if (isLongText && !isExpanded) {
            val truncated = content.take(charLimit)
            val codeBlockCount = truncated.split("```").size - 1
            if (codeBlockCount % 2 != 0) truncated + "\n```\n..." else truncated + "..."
        } else {
            content
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(modifier = Modifier.weight(0.9f, fill = false), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = bubbleColor, shape = shape, shadowElevation = 1.dp, modifier = Modifier.animateContentSize()
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .then(
                                if (isUser) Modifier.combinedClickable(
                                    onClick = { focusManager.clearFocus() },
                                    onLongClick = { showMenu = true }
                                ) else Modifier
                            )
                            .padding(14.dp)
                    ) {
                        ParsedMessageContent(displayContent, textColor, clipboardManager)

                        if (isLongText) {
                            Text(
                                text = if (isExpanded) "Show less" else "Read more",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp).clickable { isExpanded = !isExpanded }
                            )
                        }
                    }
                    
                    if (isUser) {
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Copy") }, onClick = { clipboardManager.setText(AnnotatedString(content)); showMenu = false; focusManager.clearFocus() }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(); showMenu = false; focusManager.clearFocus() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        }
                    }
                }
            }

            if (!isUser) {
                Row(modifier = Modifier.padding(top = 4.dp, start = 4.dp), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(content)); focusManager.clearFocus() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onRegenerate(); focusManager.clearFocus() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ParsedMessageContent(content: String, textColor: Color, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    val parts = content.split("```")
    Column {
        parts.forEachIndexed { index, part ->
            if (index % 2 != 0) { 
                val firstNewline = part.indexOf('\n')
                val lang = if (firstNewline != -1) part.substring(0, firstNewline).trim() else ""
                val code = if (firstNewline != -1) part.substring(firstNewline + 1).trim() else part.trim()
                
                Surface(
                    color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2D2D)).padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lang.ifEmpty { "Code" }, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(code)) }, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                        SelectionContainer { Text(text = code, color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(12.dp)) }
                    }
                }
            } else if (part.isNotBlank()) {
                SelectionContainer { MarkdownText(text = part, textColor = textColor) }
            }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean, 
    selectedUri: Uri?,
    onImageAttachClick: () -> Unit, // ছবির জন্য নতুন ফাংশন
    onFileAttachClick: () -> Unit,  // ফাইলের জন্য নতুন ফাংশন
    onRemoveAttachment: () -> Unit,
    onSendMessage: () -> Unit,
    onStopGenerating: () -> Unit
) {
    var showAttachmentMenu by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            if (selectedUri != null) {
                Box(modifier = Modifier.padding(bottom = 8.dp).size(60.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    AsyncImage(model = selectedUri, contentDescription = "Selected File", modifier = Modifier.fillMaxSize())
                    IconButton(onClick = onRemoveAttachment, modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(MaterialTheme.colorScheme.error, CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                // পপআপ মেনু অ্যাড করা হলো
                Box {
                    IconButton(onClick = { showAttachmentMenu = true }) { 
                        Icon(Icons.Default.Add, "Attach File") 
                    }
                    DropdownMenu(expanded = showAttachmentMenu, onDismissRequest = { showAttachmentMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Photos") }, 
                            onClick = { showAttachmentMenu = false; onImageAttachClick() }, 
                            leadingIcon = { Icon(Icons.Default.Image, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Files") }, 
                            onClick = { showAttachmentMenu = false; onFileAttachClick() }, 
                            leadingIcon = { Icon(Icons.Default.Folder, null) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = text, onValueChange = onTextChange,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    placeholder = { Text("Type a message...") }, maxLines = 5, shape = RoundedCornerShape(24.dp)
                )

                if (isLoading) {
                    IconButton(
                        onClick = onStopGenerating,
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, "Stop", tint = MaterialTheme.colorScheme.onError)
                    }
                } else {
                    IconButton(
                        onClick = onSendMessage,
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha1 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), "t1")
    val alpha2 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, delayMillis = 150, easing = FastOutSlowInEasing), RepeatMode.Reverse), "t2")
    val alpha3 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse), "t3")

    Row(modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Circle(alpha1); Spacer(Modifier.width(4.dp)); Circle(alpha2); Spacer(Modifier.width(4.dp)); Circle(alpha3)
    }
}
@Composable private fun Circle(alpha: Float) = Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)))
