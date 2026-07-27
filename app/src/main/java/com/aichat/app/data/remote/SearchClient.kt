package com.aichat.app.data.remote

import com.aichat.app.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SearchClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // এখন আর এখানে হার্ডকোড করা Key থাকবে না!
    // ফাংশন কল করার সময় Key পাস করতে হবে।
    suspend fun search(query: String, apiKey: String, maxResults: Int = 5): List<SearchResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw Exception("অনুগ্রহ করে অ্যাপের সেটিংসে গিয়ে Search API Key বসান")
        }

        val jsonBody = """{"q": "$query", "num": $maxResults, "gl": "bd"}"""
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://google.serper.dev/search")
            .header("X-API-KEY", apiKey.trim())
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("সার্চ এপিআই ফেইল করেছে: ${response.code}")
            }
            val responseString = response.body?.string() ?: return@use emptyList()
            parseGoogleResults(responseString)
        }
    }

    private fun parseGoogleResults(jsonString: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        try {
            val jsonObject = JSONObject(jsonString)
            
            val answerBox = jsonObject.optJSONObject("answerBox")
            if (answerBox != null) {
                val answer = answerBox.optString("answer", answerBox.optString("snippet", ""))
                if (answer.isNotBlank()) {
                    results.add(SearchResult("Google Answer", "https://google.com", answer))
                }
            }

            val organic = jsonObject.optJSONArray("organic") ?: return results
            for (i in 0 until organic.length()) {
                val item = organic.getJSONObject(i)
                val title = item.optString("title", "")
                val link = item.optString("link", "")
                val snippet = item.optString("snippet", "")

                if (title.isNotBlank() && snippet.isNotBlank()) {
                    results.add(SearchResult(title, link, snippet))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun buildSummary(results: List<SearchResult>, maxCharsPerSnippet: Int = 300): String {
        if (results.isEmpty()) return "কোনো সার্চ রেজাল্ট পাওয়া যায়নি।"

        return buildString {
            results.forEachIndexed { i, r ->
                append("${i + 1}. ${r.title}\n")
                if (r.snippet.isNotBlank()) {
                    append("   ${r.snippet.take(maxCharsPerSnippet)}\n")
                }
                append("   সোর্স: ${r.url}\n\n")
            }
        }.trim()
    }
}
