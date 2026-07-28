package com.aichat.app.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

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

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {

            AnimatedVisibility(visible = selectedUri != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Selected File",
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onRemoveAttachment,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
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

            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Box {
                    IconButton(onClick = { showAttachmentMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Attach File")
                    }
                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Photos") },
                            onClick = {
                                showAttachmentMenu = false
                                onImageAttachClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Files") },
                            onClick = {
                                showAttachmentMenu = false
                                onFileAttachClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    placeholder = { Text("Type a message...") },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp)
                )

                if (isLoading) {
                    IconButton(
                        onClick = onStopGenerating,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Generating", tint = MaterialTheme.colorScheme.onError)
                    }
                } else {
                    IconButton(
                        onClick = onSendMessage,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
