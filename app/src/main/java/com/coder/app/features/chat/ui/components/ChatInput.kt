package com.coder.app.features.chat.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    selectedUri: Uri?,
    onImageAttachClick: () -> Unit,
    onFileAttachClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSendMessage: () -> Unit,
    onStopGenerating: () -> Unit
) {
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showMyReposDialog by remember { mutableStateOf(false) } // 🚀 NEW: ডায়লগ স্টেট
    val canSend = text.isNotBlank() || selectedUri != null

    if (showMyReposDialog) {
        MyReposDialog(
            onDismiss = { showMyReposDialog = false },
            onRepoSelected = { repoName ->
                showMyReposDialog = false
                val prefix = if (text.isEmpty()) "https://github.com/$repoName" else "\nhttps://github.com/$repoName"
                onTextChange(text + prefix)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)
    ) {
        AnimatedVisibility(visible = selectedUri != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 4.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = "Selected attachment",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
                IconButton(
                    onClick = onRemoveAttachment,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 3.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(4.dp)
            ) {
                Box {
                    IconButton(onClick = { showAttachmentMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Attach")
                    }
                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Photos") },
                            onClick = { showAttachmentMenu = false; onImageAttachClick() },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Files") },
                            onClick = { showAttachmentMenu = false; onFileAttachClick() },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                        )
                        
                        // 🚀 NEW: মেনু ২ ভাগে ভাগ করা হলো
                        DropdownMenuItem(
                            text = { Text("My Repositories") },
                            onClick = { showAttachmentMenu = false; showMyReposDialog = true },
                            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Other GitHub Repo") },
                            onClick = {
                                showAttachmentMenu = false
                                val prefix = if (text.isEmpty()) "https://github.com/" else "\nhttps://github.com/"
                                onTextChange(text + prefix)
                            },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                        )
                    }
                }

                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    placeholder = { Text("Message...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    maxLines = 6,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)))
                            .togetherWith(fadeOut(tween(100)) + scaleOut(targetScale = 0.8f, animationSpec = tween(100)))
                    },
                    label = "send_stop_button"
                ) { loading ->
                    if (loading) {
                        IconButton(
                            onClick = onStopGenerating,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onError)
                        }
                    } else {
                        IconButton(
                            onClick = onSendMessage,
                            enabled = canSend,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// 🚀 NEW: রিপোজিটরি দেখানোর জন্য ডায়লগ কম্পোনেন্ট
@Composable
fun MyReposDialog(
    onDismiss: () -> Unit,
    onRepoSelected: (String) -> Unit
) {
    var repos by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val token = com.coder.app.core.network.TokenManager.githubToken
                if (token == null) {
                    error = "GitHub account is not connected. Please go to Settings and authorize GitHub first."
                    isLoading = false
                    return@launch
                }
                
                val client = com.coder.app.core.network.GithubClient(token = token)
                repos = client.getMyRepositories()
            } catch (e: Exception) {
                error = e.message ?: "Failed to load repositories"
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Select Repository") 
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Text(
                        text = error!!, 
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (repos.isEmpty()) {
                    Text("No repositories found in your account.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(repos) { repo ->
                            Text(
                                text = repo,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRepoSelected(repo) }
                                    .padding(vertical = 14.dp, horizontal = 8.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
