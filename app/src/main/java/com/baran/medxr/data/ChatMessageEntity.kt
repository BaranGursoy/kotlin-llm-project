package com.baran.medxr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single chat message.
 *
 * Stores the full conversation (system prompt, user messages,
 * and raw assistant responses) as the single source of truth.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String,       // "system", "user", or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
