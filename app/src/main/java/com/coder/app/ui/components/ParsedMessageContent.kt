package com.coder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CodeBlockShape = RoundedCornerShape(8.dp)
private val CodeBlockBackground = Color(0xFF1E1E1E)
private val CodeHeaderBackground = Color(0xFF2D2D2D)
private val CodeTextColor = Color(0xFFD4D4D4)

@Composable
fun ParsedMessageContent(
    content: String, 
    textColor: Color, 
    clipboardManager: ClipboardManager
) {
    val parsedParts = remember(content) { content.split("```") }
    
    Column {
        parsedParts.forEachIndexed { index, part ->
            if (index % 2 != 0) { 
                val firstNewline = part.indexOf('\n')
                val lang = if (firstNewline != -1) part.substring(0, firstNewline).trim() else ""
                val code = if (firstNewline != -1) part.substring(firstNewline + 1).trim() else part.trim()
                
                CodeBlockView(
                    lang = lang.ifEmpty { "Code" },
                    code = code,
                    clipboardManager = clipboardManager
                )
            } else if (part.isNotBlank()) {
                SelectionContainer { 
                    MarkdownText(text = part, textColor = textColor) 
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(
    lang: String,
    code: String,
    clipboardManager: ClipboardManager
) {
    Surface(
        color = CodeBlockBackground, 
        shape = CodeBlockShape, 
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeHeaderBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lang, 
                    color = Color.LightGray, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(code)) }, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy, 
                        contentDescription = "Copy Code", 
                        tint = Color.LightGray, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", color = Color.LightGray, fontSize = 12.sp)
                }
            }
            SelectionContainer { 
                Text(
                    text = code, 
                    color = CodeTextColor, 
                    fontFamily = FontFamily.Monospace, 
                    fontSize = 13.sp, 
                    modifier = Modifier.padding(12.dp)
                ) 
            }
        }
    }
}
