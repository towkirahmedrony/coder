package com.coder.app.features.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coder.app.core.model.ConversationEntity
import com.coder.app.core.model.MessageEntity
import com.coder.app.features.chat.data.ChatEvent
import com.coder.app.features.chat.data.ChatRepository
import com.coder.app.features.settings.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val conversations = chatRepository.getAllConversations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId = _currentConversationId.asStateFlow()
    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages = _currentMessages.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage = _streamingMessage.asStateFlow()
    private val _useCloudAi = MutableStateFlow(false)
    val useCloudAi = _useCloudAi.asStateFlow()
    private var currentChatJob: Job? = null

    init {
        viewModelScope.launch {
            _currentConversationId.collectLatest { id ->
                if (id != null) chatRepository.getMessages(id).collect { _currentMessages.value = it }
                else _currentMessages.value = emptyList()
            }
        }
        createNewChat()
    }

    fun toggleAiMode(isCloud: Boolean) { _useCloudAi.value = isCloud }
    fun selectConversation(id: String?) { _currentConversationId.value = id; _error.value = null; _streamingMessage.value = null }
    fun createNewChat() { viewModelScope.launch { selectConversation(chatRepository.createConversation("New Chat")) } }
    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversation)
            if (_currentConversationId.value == conversation.id) createNewChat()
        }
    }
    fun renameConversation(conversation: ConversationEntity, newTitle: String) { viewModelScope.launch { chatRepository.renameConversation(conversation, newTitle) } }

    fun stopGenerating() {
        currentChatJob?.cancel()
        val currentConvId = _currentConversationId.value
        val partialMessage = _streamingMessage.value
        if (currentConvId != null && partialMessage != null && partialMessage.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.insertMessage(currentConvId, "assistant", "$partialMessage \n\n*[Stopped by user]*")
                chatRepository.updateConversationTime(currentConvId)
            }
        }
        _isLoading.value = false; _streamingMessage.value = null; _error.value = null
    }

    fun deleteSingleMessage(message: MessageEntity) {}

    fun sendMessage(content: String) {
        val convId = _currentConversationId.value ?: return
        currentChatJob?.cancel()

        val githubRegex = "https://github\\.com/([^/]+)/([^/\\s]+)".toRegex()
        val match = githubRegex.find(content)

        if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix("/")
            val query = content.replace(match.value, "").trim().let { if (it.isEmpty()) "Explain the architecture and main purpose of this repository." else it }

            currentChatJob = viewModelScope.launch {
                _isLoading.value = true
                _error.value = null

                var accumulatedThoughts = "⚙️ Starting Agent..."
                _streamingMessage.value = "<think>\n$accumulatedThoughts\n"

                val conv = conversations.value.find { it.id == convId }
                if (conv?.title == "New Chat") renameConversation(conv, "$owner/$repo")
                chatRepository.insertMessage(convId, "user", content)

                try {
                    val settings = settingsRepository.settingsFlow.first()
                    val githubClient = com.coder.app.core.network.GithubClient(token = com.coder.app.core.network.TokenManager.githubToken)
                    val agent = com.coder.app.features.agent.domain.AgenticGithubProcessor(githubClient)
                    val apiClient = com.coder.app.core.network.ApiClient()

                    val finalResponse = agent.process(
                        repoOwner = owner,
                        repoName = repo,
                        userQuery = query,
                        onUpdate = { status ->
                            accumulatedThoughts += "\n⚙️ $status"
                            _streamingMessage.value = "<think>\n$accumulatedThoughts\n"
                        },
                        // 🚀 NEW: String এর বদলে List<ChatMessage> পাস করা হচ্ছে
                        aiPromptRunner = { promptMessages -> 
                            val baseUrl = if (_useCloudAi.value) settings.cloudBaseUrl else settings.localBaseUrl
                            val apiKey = if (_useCloudAi.value) settings.cloudApiKey else settings.localApiKey
                            val model = if (_useCloudAi.value) settings.cloudModelName else settings.localModelName

                            apiClient.sendChatRequest(
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                request = com.coder.app.core.model.ChatRequest(
                                    model = model,
                                    messages = promptMessages,
                                    stream = false
                                )
                            )
                        }
                    )

                    val finalMessage = "<think>\n$accumulatedThoughts\n</think>\n\n$finalResponse"
                    _streamingMessage.value = null
                    chatRepository.insertMessage(convId, "assistant", finalMessage)

                } catch (e: Exception) {
                    _streamingMessage.value = null
                    if (e !is kotlinx.coroutines.CancellationException) {
                        chatRepository.insertMessage(convId, "assistant", "❌ **Agent Error:**\n```\n${e.message}\n```")
                    }
                } finally {
                    _isLoading.value = false
                    chatRepository.updateConversationTime(convId)
                }
            }
            return
        }

        // Regular chat flow
        currentChatJob = viewModelScope.launch {
            _isLoading.value = true; _error.value = null; _streamingMessage.value = null
            val conv = conversations.value.find { it.id == convId }
            if (conv?.title == "New Chat") renameConversation(conv, content.take(30).replace("\n", " ") + if (content.length > 30) "..." else "")

            try {
                chatRepository.sendMessageSmart(convId, content, _useCloudAi.value).collect { event ->
                    when (event) {
                        is ChatEvent.Status -> _streamingMessage.value = "_${event.message}_\n"
                        is ChatEvent.Chunk -> _streamingMessage.value = event.text
                        is ChatEvent.Done -> _streamingMessage.value = null
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) _error.value = e.message ?: "Unknown error occurred"
                _streamingMessage.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
    companion object {
        fun provideFactory(chatRepository: ChatRepository, settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(chatRepository, settingsRepository) as T
            }
    }
}
