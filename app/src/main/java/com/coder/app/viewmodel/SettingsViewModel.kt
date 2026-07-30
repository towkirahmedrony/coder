package com.coder.app.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coder.app.data.model.AppSettings
import com.coder.app.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stable alias for a granular settings mutation, passed down to composables. */
typealias SettingsFieldUpdater = ((AppSettings) -> AppSettings) -> Unit

/**
 * Section-scoped, derived UI state for the General tab. Only String/Boolean
 * fields, which the Compose compiler already infers as stable — no
 * @Immutable needed for correctness, but it's added anyway for readability
 * and to make the stability contract explicit at a glance.
 */
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
    val theme: String = "system"
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * Derived, section-scoped state. distinctUntilChanged() means editing an
     * Advanced field never emits on generalUiState, so GeneralSettingsTab
     * skips recomposition entirely — the real fix for "whole tab recomposes
     * on any field change," without one StateFlow per field.
     */
    val generalUiState: StateFlow<GeneralUiState> = _settings
        .map { GeneralUiState(it.localBaseUrl, it.localApiKey, it.localModelName, it.systemPrompt) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GeneralUiState())

    val advancedUiState: StateFlow<AdvancedUiState> = _settings
        .map {
            AdvancedUiState(
                it.cloudBaseUrl, it.cloudApiKey, it.cloudModelName,
                it.searchApiKey, it.searchPrompt, it.streamResponse, it.theme
            )
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvancedUiState())

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private var saveJob: Job? = null
    private var resetJob: Job? = null
    private var lastPersisted: AppSettings = AppSettings()

    init {
        viewModelScope.launch {
            val loaded = repository.settingsFlow.first()
            _settings.value = loaded
            lastPersisted = loaded
        }
    }

    /**
     * Single source-of-truth mutation. The reducer always applies to the
     * latest in-memory state — never a UI-captured snapshot — so two fields
     * edited back-to-back can never clobber each other. Debounce lives here,
     * not in the TextField: TextField -> instant, ViewModel -> debounce,
     * Repository -> save.
     */
    fun updateField(reducer: (AppSettings) -> AppSettings) {
        val updated = reducer(_settings.value)
        _settings.value = updated
        scheduleSave(updated, immediate = false)
    }

    /** Kept for backward compatibility with any existing callers. */
    fun updateSettings(newSettings: AppSettings) = updateField { newSettings }

    /**
     * Cancels any pending debounce and persists immediately. Called on
     * focus loss and on ON_STOP so the un-saved window is bounded by
     * "however long it takes the field/screen to lose focus," not always
     * the full 500ms debounce.
     */
    fun flushNow() {
        val current = _settings.value
        if (current != lastPersisted) {
            scheduleSave(current, immediate = true)
        }
    }

    private fun scheduleSave(newSettings: AppSettings, immediate: Boolean) {
        saveJob?.cancel()
        resetJob?.cancel()
        _saveState.value = SaveState.Saving
        saveJob = viewModelScope.launch {
            if (!immediate) delay(SAVE_DEBOUNCE_MS)
            runCatching { repository.updateSettings(newSettings) }
                .onSuccess {
                    lastPersisted = newSettings
                    _saveState.value = SaveState.Saved
                    resetJob = launch {
                        delay(SAVED_INDICATOR_MS)
                        _saveState.value = SaveState.Idle
                    }
                }
                .onFailure {
                    _saveState.value = SaveState.Error(it.message ?: "Save failed")
                }
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
