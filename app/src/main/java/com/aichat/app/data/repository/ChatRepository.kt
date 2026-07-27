package com.aichat.app.data.repository

import com.aichat.app.data.local.ChatDao
import com.aichat.app.data.model.*
import com.aichat.app.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

sealed class ChatEvent {
    data class Status(val message: String) : ChatEvent()
    data class Chunk(val text: String) : ChatEvent()
    data class Done(val fullText: String) : ChatEvent()
}

class ChatRepository(
    private val chatDao: ChatDao,
    private val apiClient: ApiClient,
    private val settingsRepository: SettingsRepository,
    private val searchClient: SearchClient = SearchClient()
) {
    private val searchMarkerRegex = Regex("""(?i)<search>\s*(.*?)\s*</search>""")
    
    // Thread-safe modern date formatter
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, hh:mm a", Locale.getDefault())

    fun getAllConversations() = chatDao.getAllConversations()
    
    fun getMessages(conversationId: String) = chatDao.getMessagesForConversation(conversationId)

    suspend fun createConversation(title: String): String {
        return UUID.randomUUID().toString().also { newId ->
            chatDao.insertConversation(
                ConversationEntity(
                    id = newId,
                    title = title,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteConversation(conversation: ConversationEntity) {
        chatDao.deleteMessagesByConversation(conversation.id)
        chatDao.deleteConversation(conversation)
    }

    suspend fun renameConversation(conversation: ConversationEntity, newTitle: String) {
        chatDao.updateConversation(
            conversation.copy(
                title = newTitle,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun insertMessage(conversationId: String, role: String, content: String) {
        chatDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateConversationTime(conversationId: String) {
        chatDao.getAllConversations().first().find { it.id == conversationId }?.let {
            chatDao.updateConversation(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    private suspend fun buildHistory(
        conversationId: String,
        extraSystemMessage: String? = null,
        settingsSystemPrompt: String
    ): MutableList<ChatMessage> {
        val history = chatDao.getMessagesSync(conversationId)
            .map { ChatMessage(it.role, it.content) }
            .toMutableList()

        val currentTime = LocalDateTime.now().format(dateFormatter)
        val timeContext = "<metadata>\n[CRITICAL]: Current time is $currentTime.\n</metadata>"
        
        val systemMessageContent = buildString {
            if (settingsSystemPrompt.isNotBlank()) {
                append(settingsSystemPrompt.trim()).append("\n\n")
            }
            append(timeContext)
            if (extraSystemMessage != null) {
                append("\n\n").append(extraSystemMessage)
            }
        }
        
        history.add(0, ChatMessage("system", systemMessageContent))
        return history
    }

    fun sendMessageSmart(conversationId: String, newContent: String, useCloudAi: Boolean): Flow<ChatEvent> = flow {
        insertMessage(conversationId, "user", newContent)
        val settings = settingsRepository.settingsFlow.first()
        
        val searchInstruction = settings.searchPrompt
        val targetApiKey: String
        val targetBaseUrl: String
        val targetModelName: String
        val modeName: String

        if (useCloudAi) {
            targetApiKey = settings.cloudApiKey
            targetBaseUrl = settings.cloudBaseUrl
            targetModelName = settings.cloudModelName
            modeName = "Cloud AI"
            if (targetApiKey.isBlank()) {
                val errorMsg = "⚠️ Cloud API Key is missing. Please update in Settings."
                emit(ChatEvent.Chunk(errorMsg))
                emit(ChatEvent.Done(errorMsg))
                return@flow
            }
        } else {
            targetApiKey = settings.localApiKey
            targetBaseUrl = settings.localBaseUrl
            targetModelName = settings.localModelName
            modeName = "Local AI"
            if (targetBaseUrl.isBlank()) {
                val errorMsg = "⚠️ Local Base URL is missing. Set your Colab link."
                emit(ChatEvent.Chunk(errorMsg))
                emit(ChatEvent.Done(errorMsg))
                return@flow
            }
        }

        emit(ChatEvent.Status("⚡ Connecting to $modeName..."))

        val buffer = java.lang.StringBuilder()
        var decidedSearch = false
        var decidedNormal = false
        var searchQuery: String? = null

        try {
            val initialHistory = buildHistory(conversationId, searchInstruction, settings.systemPrompt)
            val chatRequest = ChatRequest(targetModelName, initialHistory, true)
            
            apiClient.streamChatRequest(targetBaseUrl, targetApiKey, chatRequest)
                .takeWhile { !decidedSearch }
                .collect { piece ->
                    buffer.append(piece)
                    val currentText = buffer.toString()
                    
                    if (!decidedNormal && !decidedSearch) {
                        val match = searchMarkerRegex.find(currentText)
                        if (match != null) {
                            decidedSearch = true
                            searchQuery = match.groupValues[1].trim()
                        } else if (currentText.length > 40 && !currentText.uppercase().contains("<SEARCH>")) {
                            decidedNormal = true
                        }
                    }
                    
                    if (decidedNormal && settings.streamResponse) {
                        emit(ChatEvent.Chunk(currentText))
                    } else if (!decidedSearch) {
                        emit(ChatEvent.Status("Thinking..."))
                    }
                }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (!decidedSearch) {
                val errorReply = "⚠️ $modeName Error: ${e.message}"
                emit(ChatEvent.Chunk(errorReply))
                emit(ChatEvent.Done(errorReply))
                return@flow
            }
        }

        val collectedText = buffer.toString()
        if (!decidedSearch) {
            if (collectedText.isNotBlank()) {
                insertMessage(conversationId, "assistant", collectedText)
                updateConversationTime(conversationId)
            }
            emit(ChatEvent.Done(collectedText))
            return@flow
        }

        emit(ChatEvent.Status("🔍 Searching: ${searchQuery ?: ""}"))
        
        var searchError = ""
        val summary = try { 
            val rawData = searchClient.search(searchQuery ?: "", settings.searchApiKey)
            val parsedData = searchClient.buildSummary(rawData)
            parsedData.ifBlank {
                searchError = "Serper API returned empty data."
                ""
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            searchError = e.message ?: "Unknown Network Error"
            "" 
        }

        if (searchError.isNotEmpty()) {
            val errorReply = "⚠️ **System Error: Search Failed!**\n\n**Log:** `$searchError`\n\n*(AI generation automatically stopped)*"
            insertMessage(conversationId, "assistant", errorReply)
            emit(ChatEvent.Chunk(errorReply))
            emit(ChatEvent.Done(errorReply))
            return@flow
        }
        
        emit(ChatEvent.Status("Generating response..."))

        val finalHistory = buildHistory(
            conversationId, 
            """
            User question: $newContent
            Search Query: ${searchQuery ?: ""}
            Search Results: $summary
            
            CRITICAL INSTRUCTION: Answer the user's question USING ONLY the facts provided in the 'Search Results'. Do NOT guess or make up facts.
            """.trimIndent(), 
            settings.systemPrompt
        )
        
        try {
            if (settings.streamResponse) {
                val finalBuffer = java.lang.StringBuilder()
                val finalRequest = ChatRequest(targetModelName, finalHistory, true)
                
                apiClient.streamChatRequest(targetBaseUrl, targetApiKey, finalRequest).collect { piece ->
                    finalBuffer.append(piece)
                    emit(ChatEvent.Chunk(finalBuffer.toString()))
                }
                
                val finalFullResponse = finalBuffer.toString()
                if (finalFullResponse.isNotBlank()) {
                    insertMessage(conversationId, "assistant", finalFullResponse)
                    updateConversationTime(conversationId)
                }
                emit(ChatEvent.Done(finalFullResponse))
            } else {
                val finalRequest = ChatRequest(targetModelName, finalHistory, false)
                val finalResponse = apiClient.sendChatRequest(targetBaseUrl, targetApiKey, finalRequest)
                
                if (finalResponse.isNotBlank()) {
                    insertMessage(conversationId, "assistant", finalResponse)
                    updateConversationTime(conversationId)
                }
                emit(ChatEvent.Chunk(finalResponse))
                emit(ChatEvent.Done(finalResponse))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val errorReply = "⚠️ $modeName Error: ${e.message}"
            emit(ChatEvent.Chunk(errorReply))
            emit(ChatEvent.Done(errorReply))
        }
    }
}
