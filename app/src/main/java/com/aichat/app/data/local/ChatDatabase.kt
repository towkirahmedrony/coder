package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.aichat.app.data.model.ConversationEntity
import com.aichat.app.data.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        ConversationEntity::class, 
        MessageEntity::class
    ], 
    version = 1, 
    exportSchema = false // See suggestions: Should be true in production
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

@Dao
interface ChatDao {

    // =========================================================================
    // CONVERSATION OPERATIONS
    // =========================================================================

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)


    // =========================================================================
    // MESSAGE OPERATIONS
    // =========================================================================

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesSync(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)


    // =========================================================================
    // TRANSACTIONS (ATOMIC OPERATIONS)
    // =========================================================================

    /**
     * Safely deletes a conversation and all its associated messages in a single atomic transaction.
     * Prevents orphaned messages and storage leaks if the operation fails midway.
     */
    @Transaction
    suspend fun deleteConversationAndMessages(conversation: ConversationEntity, conversationId: String) {
        deleteMessagesByConversation(conversationId)
        deleteConversation(conversation)
    }
}
