package com.aichat.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    val context = LocalContext.current
    val parts = text.split("```")
    
    Column(modifier = modifier) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) { // Code block
                val lines = part.trim().split("\n")
                val language = lines.firstOrNull()?.trim() ?: ""
                val code = if (language.isNotEmpty() && !language.contains(" ")) {
                    lines.drop(1).joinToString("\n")
                } else {
                    part.trim()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2B2B2B))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = language, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied Code", code)
                                clipboard.setPrimaryClip(clip)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = Color.LightGray)
                            }
                        }
                        Text(
                            text = code,
                            color = Color(0xFFA9B7C6),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            } else { // Normal text, basic inline bold formatting
                if (part.isNotEmpty()) {
                    Text(
                        text = parseInlineMarkdown(part),
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    var lastIndex = 0
    boldRegex.findAll(text).forEach { matchResult ->
        append(text.substring(lastIndex, matchResult.range.first))
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(matchResult.groupValues[1])
        }
        lastIndex = matchResult.range.last + 1
    }
    append(text.substring(lastIndex))
}
