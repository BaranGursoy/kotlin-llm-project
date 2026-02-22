package com.baran.medxr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [ChatMessageEntity].
 *
 * Provides a reactive [Flow] for the UI to observe, and suspend
 * functions for inserting messages from the repository layer.
 */
@Dao
interface ChatMessageDao {

    /**
     * Observe the full conversation history ordered by timestamp.
     * The system prompt (seeded on first launch) will always be first.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    /**
     * Get all messages synchronously — used by the repository to
     * build the full prompt history for the Groq API request.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    /** Insert a single message (user, assistant, or system). */
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    /** Check if the database has any rows (used for seeding). */
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun count(): Int
    /** Delete all messages (used for "Clear Chat"). */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
