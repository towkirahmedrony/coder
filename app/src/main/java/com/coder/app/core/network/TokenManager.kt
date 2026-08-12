package com.coder.app.core.network

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TokenManager {
    // 🚀 NEW: UI কে আপডেট করার জন্য স্টেট ফ্লো
    private val _isGithubConnected = MutableStateFlow(false)
    val isGithubConnected: StateFlow<Boolean> = _isGithubConnected

    var githubToken: String? = null

    fun init(context: Context) { 
        githubToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getString("gh_token", null) 
        _isGithubConnected.value = githubToken != null
    }
    
    fun saveToken(context: Context, token: String) {
        githubToken = token
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().putString("gh_token", token).apply()
        _isGithubConnected.value = true
    }
    
    fun logout(context: Context) {
        githubToken = null
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().remove("gh_token").apply()
        _isGithubConnected.value = false
    }
}
