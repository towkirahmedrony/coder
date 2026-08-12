package com.coder.app.features.chat.data

import android.content.Context
import android.net.Uri
import com.coder.app.features.chat.data.AttachmentProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MessageProcessor {
    
    suspend fun processAndSend(
        context: Context,
        uri: Uri?,
        text: String,
        onProcessingStateChange: (Boolean) -> Unit,
        onSend: (String) -> Unit
    ) {
        if (uri == null && text.isBlank()) return

        if (uri != null) {
            onProcessingStateChange(true)
            try {
                // Moving heavy OCR processing to background thread
                val attachmentText = withContext(Dispatchers.IO) {
                    AttachmentProcessor.processUri(context, uri)
                }
                val finalMessage = "$attachmentText$text"
                onSend(finalMessage)
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful degradation: send the prompt text even if OCR fails
                onSend(text)
            } finally {
                onProcessingStateChange(false)
            }
        } else {
            onSend(text)
        }
    }
}
