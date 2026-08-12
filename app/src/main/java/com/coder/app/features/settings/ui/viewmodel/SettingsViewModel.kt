package com.coder.app.features.settings.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coder.app.core.model.AppSettings
import com.coder.app.features.settings.data.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias SettingsFieldUpdater = ((AppSettings) -> AppSettings) -> Unit

@Immutable
data class GeneralUiState(
    val localBaseUrl: String = "",
    val localApiKey: String = "",
    val localModelName: String = "",
    val systemPrompt: String = ""
)

@Immutable
data class AdvancedUiState(
    val cloudBaseUrl: String = "",
    val cloudApiKey: String = "",
    val cloudModelName: String = "",
    val searchApiKey: String = "",
    val searchPrompt: String = "",
    val streamResponse: Boolean = false,
    val theme: String = "system",
    val githubClientId: String = "",
    val githubClientSecret: String = ""
)

@OptIn(FlowPreview::class)
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val generalUiState: StateFlow<GeneralUiState> = _settings
        .map { GeneralUiState(it.localBaseUrl, it.localApiKey, it.localModelName, it.systemPrompt) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GeneralUiState())

    val advancedUiState: StateFlow<AdvancedUiState> = _settings
        .map {
            AdvancedUiState(
                it.cloudBaseUrl, it.cloudApiKey, it.cloudModelName,
                it.searchApiKey, it.searchPrompt, it.streamResponse, it.theme,
                it.githubClientId, it.githubClientSecret
            )
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvancedUiState())

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val saveIntentFlow = MutableSharedFlow<AppSettings>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var lastPersisted: AppSettings = AppSettings()

    init {
        viewModelScope.launch {
            val loaded = repository.settingsFlow.first()
            _settings.value = loaded
            lastPersisted = loaded

            saveIntentFlow
                .debounce(SAVE_DEBOUNCE_MS)
                .collectLatest { settingsToSave ->
                    performSave(settingsToSave)
                }
        }
    }

    fun updateField(reducer: (AppSettings) -> AppSettings) {
        _settings.update { current ->
            val updated = reducer(current)
            if (updated != current) {
                saveIntentFlow.tryEmit(updated)
            }
            updated
        }
    }

    fun updateSettings(newSettings: AppSettings) = updateField { newSettings }

    fun flushNow() {
        val current = _settings.value
        if (current != lastPersisted) {
            viewModelScope.launch { performSave(current) }
        }
    }

    private suspend fun performSave(newSettings: AppSettings) {
        if (newSettings == lastPersisted) return

        _saveState.value = SaveState.Saving
        runCatching { repository.updateSettings(newSettings) }
            .onSuccess {
                lastPersisted = newSettings
                _saveState.value = SaveState.Saved
                delay(SAVED_INDICATOR_MS)
                _saveState.compareAndSet(SaveState.Saved, SaveState.Idle)
            }
            .onFailure {
                _saveState.value = SaveState.Error(it.message ?: "Save failed")
            }
    }

    sealed interface SaveState {
        data object Idle : SaveState
        data object Saving : SaveState
        data object Saved : SaveState
        data class Error(val message: String) : SaveState
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 500L
        private const val SAVED_INDICATOR_MS = 2_000L

        fun provideFactory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
