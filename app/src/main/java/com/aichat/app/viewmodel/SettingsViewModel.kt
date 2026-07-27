package com.aichat.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.AppSettings
import com.aichat.app.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    // UI-কে সাথে সাথে আপডেট করার জন্য লোকাল স্টেট
    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

    private var saveJob: Job? = null

    init {
        // অ্যাপ ওপেন হলে ডাটাবেস থেকে শুধু একবার ডেটা লোড করবে
        viewModelScope.launch {
            _settings.value = repository.settingsFlow.first()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        // ১. কার্সরের লাফালাফি এড়াতে সাথে সাথে UI আপডেট করবে
        _settings.value = newSettings

        // ২. টাইপ করার সময় প্রতি ক্লিকে ডাটাবেস সেভ না করে আধা সেকেন্ড অপেক্ষা করবে
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            repository.updateSettings(newSettings)
        }
    }

    companion object {
        fun provideFactory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
