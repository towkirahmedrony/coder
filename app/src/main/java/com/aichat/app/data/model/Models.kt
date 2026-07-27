package com.aichat.app.data.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Entity(
    tableName = "conversations",
    indices = [Index(value = ["updated_at"])]
)
data class ConversationEntity(
    @PrimaryKey 
    @ColumnInfo(name = "id") val id: String,
    
    @ColumnInfo(name = "title") val title: String,
    
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Keep
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversation_id"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey 
    @ColumnInfo(name = "id") val id: String,
    
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    
    @ColumnInfo(name = "role") val role: String,
    
    @ColumnInfo(name = "content") val content: String,
    
    @ColumnInfo(name = "timestamp") val timestamp: Long
)

@Keep
data class AppSettings(
    val localBaseUrl: String = "https://your-colab-link/v1",
    val localApiKey: String = "sk-1234",
    val localModelName: String = "qwen14b",
    
    val cloudBaseUrl: String = "https://api.groq.com/openai/v1",
    val cloudApiKey: String = "",
    val cloudModelName: String = "llama-3.3-70b-versatile",
    
    val systemPrompt: String = "", 
    val searchPrompt: String = "",
    val streamResponse: Boolean = true,
    val theme: String = "system",
    val searchApiKey: String = ""
)

@Keep
@Serializable
data class ChatMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Keep
@Serializable
data class ChatRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessage>,
    @SerialName("stream") val stream: Boolean
)

@Keep
@Serializable
data class ChatResponse(
    @SerialName("choices") val choices: List<Choice>
) {
    @Keep
    @Serializable
    data class Choice(
        @SerialName("message") val message: ChatMessage
    )
}

@Keep
@Serializable
data class ChatStreamChunk(
    @SerialName("choices") val choices: List<StreamChoice>
) {
    @Keep
    @Serializable
    data class StreamChoice(
        @SerialName("delta") val delta: Delta
    )

    @Keep
    @Serializable
    data class Delta(
        @SerialName("content") val content: String? = null
    )
}
