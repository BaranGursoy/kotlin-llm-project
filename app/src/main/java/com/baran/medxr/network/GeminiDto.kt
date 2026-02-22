package com.baran.medxr.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs modelling the OpenAI-compatible chat/completions API (used by Groq).
 *
 * Request shape:
 * {
 *   "model": "llama-3.3-70b-versatile",
 *   "messages": [{ "role": "user", "content": "..." }]
 * }
 *
 * Response shape (simplified):
 * {
 *   "choices": [{ "message": { "role": "assistant", "content": "..." } }]
 * }
 */

// ── Request ────────────────────────────────────────────────────────────────────

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

// ── Response ───────────────────────────────────────────────────────────────────

@Serializable
data class ChatResponse(
    val choices: List<Choice>? = null
)

@Serializable
data class Choice(
    val message: ChatMessage
)
