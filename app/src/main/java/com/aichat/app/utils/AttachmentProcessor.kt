package com.aichat.app.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object AttachmentProcessor {

    suspend fun processUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        if (mimeType.startsWith("image/")) {
            processImage(context, uri)
        } else {
            processFile(context, uri)
        }
    }

    private suspend fun processImage(context: Context, uri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume("Extracted image text:\n```\n${visionText.text}\n```\n")
                }
                .addOnFailureListener {
                    // Fail silently to allow normal text message to proceed
                    continuation.resume("") 
                }
                .addOnCompleteListener {
                    recognizer.close()
                }
        } catch (e: Exception) {
            continuation.resume("")
        }
    }

    private fun processFile(context: Context, uri: Uri): String {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            "File content:\n```\n$content\n```\n"
        } catch (e: Exception) {
            "Error reading file content.\n"
        }
    }
}
