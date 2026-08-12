package com.coder.app.features.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.coder.app.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val LOCAL_BASE_URL = stringPreferencesKey("local_base_url")
        private val LOCAL_API_KEY = stringPreferencesKey("local_api_key")
        private val LOCAL_MODEL_NAME = stringPreferencesKey("local_model_name")

        private val CLOUD_BASE_URL = stringPreferencesKey("cloud_base_url")
        private val CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
        private val CLOUD_MODEL_NAME = stringPreferencesKey("cloud_model_name")

        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val SEARCH_PROMPT = stringPreferencesKey("search_prompt")
        private val STREAM_RESPONSE = booleanPreferencesKey("stream_response")
        private val THEME = stringPreferencesKey("theme")
        private val SEARCH_API_KEY = stringPreferencesKey("search_api_key")

        // 🚀 NEW: GitHub Preference Keys
        private val GITHUB_CLIENT_ID = stringPreferencesKey("github_client_id")
        private val GITHUB_CLIENT_SECRET = stringPreferencesKey("github_client_secret")

        private val DEFAULT_SEARCH_PROMPT = """
            <system_rules>
            You are a smart AI assistant. However, you MUST NOT answer factual questions using your internal memory.
            CRITICAL: For ANY question about specific people, celebrities, current events, sports, or real-world facts, YOU MUST USE THE SEARCH TOOL.

            To search, you MUST reply with EXACTLY this format and ABSOLUTELY NO OTHER TEXT:
            <search>your query here</search>

            EXAMPLES OF HOW YOU MUST BEHAVE:
            User: who is tanzid tamim?
            Assistant: <search>Tanzid Tamim cricketer</search>

            User: who is tahsan khan?
            Assistant: <search>Tahsan Khan Bangladeshi singer actor biography</search>

            WARNING: Do NOT attempt to guess facts. NEVER write biographies from memory. ALWAYS use the <search> tag first for such queries.
            </system_rules>
        """.trimIndent()
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppSettings(
                localBaseUrl = prefs[LOCAL_BASE_URL] ?: "",
                localApiKey = prefs[LOCAL_API_KEY] ?: "",
                localModelName = prefs[LOCAL_MODEL_NAME] ?: "qwen14b",

                cloudBaseUrl = prefs[CLOUD_BASE_URL] ?: "https://api.groq.com/openai/v1",
                cloudApiKey = prefs[CLOUD_API_KEY] ?: "",
                cloudModelName = prefs[CLOUD_MODEL_NAME] ?: "llama-3.3-70b-versatile",

                systemPrompt = prefs[SYSTEM_PROMPT] ?: "",
                searchPrompt = prefs[SEARCH_PROMPT] ?: DEFAULT_SEARCH_PROMPT,
                streamResponse = prefs[STREAM_RESPONSE] ?: true,
                theme = prefs[THEME] ?: "system",
                searchApiKey = prefs[SEARCH_API_KEY] ?: "",

                githubClientId = prefs[GITHUB_CLIENT_ID] ?: "",
                githubClientSecret = prefs[GITHUB_CLIENT_SECRET] ?: ""
            )
        }
        .distinctUntilChanged()

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            if (prefs[LOCAL_BASE_URL] != settings.localBaseUrl) prefs[LOCAL_BASE_URL] = settings.localBaseUrl
            if (prefs[LOCAL_API_KEY] != settings.localApiKey) prefs[LOCAL_API_KEY] = settings.localApiKey
            if (prefs[LOCAL_MODEL_NAME] != settings.localModelName) prefs[LOCAL_MODEL_NAME] = settings.localModelName

            if (prefs[CLOUD_BASE_URL] != settings.cloudBaseUrl) prefs[CLOUD_BASE_URL] = settings.cloudBaseUrl
            if (prefs[CLOUD_API_KEY] != settings.cloudApiKey) prefs[CLOUD_API_KEY] = settings.cloudApiKey
            if (prefs[CLOUD_MODEL_NAME] != settings.cloudModelName) prefs[CLOUD_MODEL_NAME] = settings.cloudModelName

            if (prefs[SYSTEM_PROMPT] != settings.systemPrompt) prefs[SYSTEM_PROMPT] = settings.systemPrompt
            if (prefs[SEARCH_PROMPT] != settings.searchPrompt) prefs[SEARCH_PROMPT] = settings.searchPrompt
            if (prefs[STREAM_RESPONSE] != settings.streamResponse) prefs[STREAM_RESPONSE] = settings.streamResponse
            if (prefs[THEME] != settings.theme) prefs[THEME] = settings.theme
            if (prefs[SEARCH_API_KEY] != settings.searchApiKey) prefs[SEARCH_API_KEY] = settings.searchApiKey

            if (prefs[GITHUB_CLIENT_ID] != settings.githubClientId) prefs[GITHUB_CLIENT_ID] = settings.githubClientId
            if (prefs[GITHUB_CLIENT_SECRET] != settings.githubClientSecret) prefs[GITHUB_CLIENT_SECRET] = settings.githubClientSecret
        }
    }
}
