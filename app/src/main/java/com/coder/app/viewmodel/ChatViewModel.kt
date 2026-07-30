package com.coder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coder.app.data.model.ConversationEntity
import com.coder.app.data.model.MessageEntity
import com.coder.app.data.repository.ChatEvent
import com.coder.app.data.repository.ChatRepository
import com.coder.app.data.repository.SettingsRepository
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
    
    // 🚀 NEW: UI থেকে মডেল সুইচ করার জন্য স্টেট
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
        viewModelScope.launch {
            val convs = conversations.first()
            if (convs.isNotEmpty()) _currentConversationId.value = convs.first().id
        }
    }

    // 🚀 NEW: চ্যাট স্ক্রিনের ড্রপডাউন থেকে মডেল চেঞ্জ করার ফাংশন
    fun toggleAiMode(isCloud: Boolean) {
        _useCloudAi.value = isCloud
    }

    fun selectConversation(id: String?) { _currentConversationId.value = id; _error.value = null; _streamingMessage.value = null }
    fun createNewChat() { viewModelScope.launch { selectConversation(chatRepository.createConversation("New Chat")) } }

    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversation)
            if (_currentConversationId.value == conversation.id) selectConversation(conversations.value.firstOrNull { it.id != conversation.id }?.id)
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
        _isLoading.value = false
        _streamingMessage.value = null
        _error.value = null
    }

    fun deleteSingleMessage(message: MessageEntity) {
        // Optional: chatRepository.deleteMessage(message.id)
    }

    fun sendMessage(content: String) {
        val convId = _currentConversationId.value ?: return

        currentChatJob?.cancel()

        currentChatJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _streamingMessage.value = null

            val conv = conversations.value.find { it.id == convId }
            if (conv?.title == "New Chat") renameConversation(conv, content.take(30).replace("\n", " ") + if (content.length > 30) "..." else "")

            try {
                // 🚀 UPDATE: _useCloudAi.value প্যারামিটারটি পাঠানো হচ্ছে
                chatRepository.sendMessageSmart(convId, content, _useCloudAi.value).collect { event ->
                    when (event) {
                        is ChatEvent.Status -> _streamingMessage.value = "_${event.message}_\n"
                        is ChatEvent.Chunk -> _streamingMessage.value = event.text
                        is ChatEvent.Done -> _streamingMessage.value = null
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _error.value = e.message ?: "Unknown error occurred"
                }
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
