package com.coder.app.core.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GithubClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val token: String? = null
) {
    suspend fun exchangeCodeForToken(clientId: String, clientSecret: String, code: String): String? = withContext(Dispatchers.IO) {
        val url = "https://github.com/login/oauth/access_token"
        val requestBody = FormBody.Builder().add("client_id", clientId).add("client_secret", clientSecret).add("code", code).build()
        val request = Request.Builder().url(url).post(requestBody).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val json = JSONObject(response.body?.string() ?: return@withContext null)
            if (json.has("access_token")) json.getString("access_token") else null
        }
    }

    suspend fun getMyRepositories(): List<String> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/user/repos?sort=updated&per_page=100"
        val requestBuilder = Request.Builder().url(url).header("Accept", "application/vnd.github.v3+json").header("User-Agent", "CoderApp-Agent/1.0")
        token?.let { requestBuilder.header("Authorization", "Bearer $it") } ?: throw IOException("GitHub token missing.")
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Code ${response.code}")
            val jsonArray = JSONArray(response.body?.string() ?: throw IOException("Empty response"))
            val repos = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) repos.add(jsonArray.getJSONObject(i).getString("full_name"))
            repos
        }
    }

    // 🚀 NEW: ডায়নামিকভাবে ডিফল্ট ব্রাঞ্চ (main/master) বের করার ফাংশন
    suspend fun getDefaultBranch(owner: String, repo: String): String = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo"
        val requestBuilder = Request.Builder().url(url).header("Accept", "application/vnd.github.v3+json").header("User-Agent", "CoderApp-Agent/1.0")
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) return@withContext "main" // fallback
            val json = JSONObject(response.body?.string() ?: return@withContext "main")
            json.optString("default_branch", "main")
        }
    }

    suspend fun getRepositoryTree(owner: String, repo: String, branch: String): String = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/git/trees/$branch?recursive=1"
        val requestBuilder = Request.Builder().url(url).header("Accept", "application/vnd.github.v3+json").header("User-Agent", "CoderApp-Agent/1.0")
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Code ${response.code}: API Limit or Private Repo")
            val json = JSONObject(response.body?.string() ?: throw IOException("Empty response"))
            val treeArray = json.getJSONArray("tree")
            val treeBuilder = java.lang.StringBuilder()
            for (i in 0 until treeArray.length()) {
                val item = treeArray.getJSONObject(i)
                treeBuilder.append("- ${item.getString("path")} (${item.getString("type")})\n")
            }
            treeBuilder.toString()
        }
    }

    suspend fun getFileContent(owner: String, repo: String, path: String): String = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/contents/$path"
        val requestBuilder = Request.Builder().url(url).header("Accept", "application/vnd.github.v3+json").header("User-Agent", "CoderApp-Agent/1.0")
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Code ${response.code}")
            val json = JSONObject(response.body?.string() ?: throw IOException("Empty response"))
            val decodedBytes = Base64.decode(json.getString("content").replace("\n", ""), Base64.DEFAULT)
            String(decodedBytes)
        }
    }
}
