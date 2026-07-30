package com.coder.app.ui.components.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.coder.app.ui.components.ParsedMessageContent

@Composable
fun AssistantMessageRow(
    content: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean,
    onRegenerate: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        MessageAvatar(isUser = false)
        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize()
        ) {
            // Fixed: Using textColor instead of color
            ParsedMessageContent(
                content = content,
                textColor = MaterialTheme.colorScheme.onSurface,
                clipboardManager = clipboardManager
            )

            if (isStreaming) {
                StreamingCursor()
            }

            if (content.isNotBlank() && !isStreaming) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MessageActionChip(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        onClick = {
                            clipboardManager.setText(AnnotatedString(content))
                            focusManager.clearFocus()
                        }
                    )
                    MessageActionChip(
                        icon = Icons.Default.Refresh,
                        label = "Regenerate",
                        onClick = {
                            onRegenerate()
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}
