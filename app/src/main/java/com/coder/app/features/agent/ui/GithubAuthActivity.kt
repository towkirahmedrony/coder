package com.coder.app.features.agent.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.coder.app.App
import com.coder.app.core.network.GithubClient
import com.coder.app.core.network.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GithubAuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri != null && uri.scheme == "coderapp" && uri.host == "github-callback") {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                Toast.makeText(this, "Connecting to GitHub...", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    try {
                        // 🚀 SettingsRepo থেকে ডায়নামিক Client ID & Secret রিড করা হচ্ছে
                        val settingsRepo = (application as App).container.settingsRepository
                        val settings = settingsRepo.settingsFlow.first()

                        val clientId = settings.githubClientId.trim()
                        val clientSecret = settings.githubClientSecret.trim()

                        if (clientId.isBlank() || clientSecret.isBlank()) {
                            Toast.makeText(this@GithubAuthActivity, "Client ID or Secret is missing in Settings!", Toast.LENGTH_LONG).show()
                            finish()
                            return@launch
                        }

                        val client = GithubClient()
                        val token = client.exchangeCodeForToken(clientId, clientSecret, code)
                        if (token != null) {
                            TokenManager.saveToken(this@GithubAuthActivity, token)
                            Toast.makeText(this@GithubAuthActivity, "GitHub Connected Successfully! 🎉", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@GithubAuthActivity, "Failed to exchange token. Check Client Secret.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@GithubAuthActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    finish()
                }
            } else finish()
        } else finish()
    }
}
