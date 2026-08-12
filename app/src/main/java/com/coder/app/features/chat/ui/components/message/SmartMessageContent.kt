package com.coder.app.features.chat.ui.components.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.app.features.chat.ui.components.MarkdownText

data class MessagePart(val text: String, val isThink: Boolean, val isClosed: Boolean)

fun parseThinkBlocks(content: String): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()
    var currentIndex = 0
    while (currentIndex < content.length) {
        val startIndex = content.indexOf("<think>", currentIndex)
        if (startIndex == -1) {
            parts.add(MessagePart(content.substring(currentIndex), false, true))
            break
        }
        if (startIndex > currentIndex) {
            parts.add(MessagePart(content.substring(currentIndex, startIndex), false, true))
        }
        val endIndex = content.indexOf("</think>", startIndex + 7)
        if (endIndex == -1) {
            parts.add(MessagePart(content.substring(startIndex + 7), true, false))
            break
        } else {
            parts.add(MessagePart(content.substring(startIndex + 7, endIndex), true, true))
            currentIndex = endIndex + 8
        }
    }
    return parts.filter { it.text.isNotBlank() }
}

@Composable
fun SmartMessageContent(content: String, modifier: Modifier = Modifier) {
    val parts = remember(content) { parseThinkBlocks(content) }

    Column(modifier = modifier.fillMaxWidth()) {
        parts.forEach { part ->
            if (part.isThink) {
                ThinkAccordion(
                    thinkText = part.text.trim(),
                    isStreaming = !part.isClosed // ট্যাগ ক্লোজ না হলে ধরে নেব এখনও ভাবছে
                )
            } else {
                MarkdownText(text = part.text.trim())
            }
        }
    }
}

@Composable
fun ThinkAccordion(thinkText: String, isStreaming: Boolean) {
    // স্ট্রিমিং চলা অবস্থায় ওপেন থাকবে, স্ট্রিমিং শেষ হলে একা একাই কলাপ্স হয়ে যাবে
    var manuallyExpanded by remember { mutableStateOf<Boolean?>(null) }
    val isExpanded = manuallyExpanded ?: isStreaming
    
    // মডেল যদি জেনারেট করা শেষ করে (isStreaming = false), তাহলে অটো কলাপ্স হবে, তবে ইউজার চাইলে আবার খুলতে পারবে
    LaunchedEffect(isStreaming) {
        if (!isStreaming && manuallyExpanded == null) {
            manuallyExpanded = false
        }
    }

    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "icon_rotation")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { manuallyExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isStreaming) "Thinking..." else "Thought process",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle Thought Process",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp).rotate(rotation)
            )
        }
        
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = thinkText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}
