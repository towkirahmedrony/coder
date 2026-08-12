package com.coder.app.core.network

import com.coder.app.core.model.ChatRequest
import com.coder.app.core.model.ChatResponse
import com.coder.app.core.model.ChatStreamChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiClient {
    private val client = OkHttpClient.Builder()
        // টাইমআউট লিমিট বাড়িয়ে দেওয়া হলো
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS) // উত্তর আসার জন্য ৫ মিনিট পর্যন্ত অপেক্ষা করবে
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS) // কানেকশন যেন ড্রপ না করে তাই বারবার পিং করবে
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("ngrok-skip-browser-warning", "true")
                    .header("User-Agent", "CoderApp/1.0")
                    .header("Origin", "http://localhost")
                    .build()
            )
        }
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private fun getErrorMessage(code: Int, body: String?, url: String): String =
        if (body?.trim().isNullOrEmpty()) "Error $code at $url" else if (body!!.contains("<html", true)) "Error $code: Blocked by Proxy" else "Error $code: ${body.take(150)}"

    suspend fun sendChatRequest(baseUrl: String, apiKey: String, request: ChatRequest): String = withContext(Dispatchers.IO) {
        val targetUrl = "${baseUrl.trim().removeSuffix("/")}/chat/completions"
        val requestBody = json.encodeToString(request.copy(model = request.model.trim())).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder().url(targetUrl).header("Authorization", "Bearer ${apiKey.trim()}").post(requestBody).build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) throw Exception(getErrorMessage(response.code, response.body?.string(), targetUrl))
            json.decodeFromString<ChatResponse>(response.body?.string() ?: throw Exception("Empty response")).choices.firstOrNull()?.message?.content ?: throw Exception("No content")
        }
    }

    fun streamChatRequest(baseUrl: String, apiKey: String, request: ChatRequest): Flow<String> = flow {
        val targetUrl = "${baseUrl.trim().removeSuffix("/")}/chat/completions"
        val requestBody = json.encodeToString(request.copy(model = request.model.trim())).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder().url(targetUrl).header("Authorization", "Bearer ${apiKey.trim()}").header("Accept", "text-event-stream").post(requestBody).build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) throw Exception(getErrorMessage(response.code, response.body?.string(), targetUrl))
            val source = response.body?.source() ?: throw Exception("Empty response body")
            while (!source.exhausted()) {
                val line = source.readUtf8Line()?.trim() ?: continue
                if (line.startsWith("data:") && line.removePrefix("data:").trim().let { it != "[DONE]" && it.isNotEmpty() }) {
                    try { json.decodeFromString<ChatStreamChunk>(line.removePrefix("data:").trim()).choices.firstOrNull()?.delta?.content?.let { emit(it) } } catch (e: Exception) {}
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
