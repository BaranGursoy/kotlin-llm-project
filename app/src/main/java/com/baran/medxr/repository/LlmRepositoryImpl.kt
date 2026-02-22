package com.baran.medxr.repository

import com.baran.medxr.data.ChatMessageDao
import com.baran.medxr.data.ChatMessageEntity
import com.baran.medxr.model.AgentResponse
import com.baran.medxr.network.ChatMessage
import com.baran.medxr.network.ChatRequest
import com.baran.medxr.network.GeminiApiService
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Production implementation of [LlmRepository].
 *
 * Uses [ChatMessageDao] as the **single source of truth** for
 * conversation history. All messages (system, user, assistant)
 * are persisted in Room before being sent to the Groq API.
 *
 * @param apiService  Retrofit service — constructor-injected for testability.
 * @param apiKey      Groq API key — constructor-injected so it can be swapped.
 * @param dao         Room DAO — the single source of truth for chat history.
 */
class LlmRepositoryImpl(
    private val apiService: GeminiApiService,
    private val apiKey: String,
    private val dao: ChatMessageDao
) : LlmRepository {

    companion object {
        private const val MODEL = "llama-3.3-70b-versatile"
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1_000L
        internal const val SYSTEM_PROMPT = """
    You are the mobile triage agent for a Medical XR Simulation system. 
    Your goal is to converse naturally with the user, assess their symptoms, and prepare a data payload for their upcoming VR avatar consultation.
    
    CRITICAL RULE: Act like a real human. Do NOT repeat medical disclaimers (e.g., "this is for informational purposes" or "consult a doctor") in your responses. Be concise, empathetic, and highly conversational.
    
    You MUST respond strictly in the following JSON format and nothing else:
    {
        "patient_message": "Your natural, conversational response to the user. No disclaimers.",
        "xr_scene_recommendation": "Identify the relevant medical department (e.g., Cardiology, Neurology, General).",
        "urgency_level": "One of: Low, Medium, or High",
        "avatar_emotion_trigger": "Suggest an emotion for the VR avatar (e.g., calming, attentive, urgent)."
    }
    
    Do NOT include any text outside the JSON object. Do NOT wrap it in markdown code fences. Return ONLY valid JSON.
"""
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Seeds the database with the system prompt if it's empty.
     * Called once from [MainActivity] after constructing the repository.
     */
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insertMessage(
                ChatMessageEntity(
                    role = "system",
                    content = SYSTEM_PROMPT.trimIndent()
                )
            )
        }
    }

    override suspend fun ask(prompt: String, displayPrompt: String): Result<AgentResponse> = runCatching {
        // 1. Insert the CLEAN user message into the database (for display)
        dao.insertMessage(
            ChatMessageEntity(role = "user", content = displayPrompt)
        )

        // 2. Query the full conversation history from the database
        val history = dao.getAll().map { entity ->
            ChatMessage(role = entity.role, content = entity.content)
        }.toMutableList()

        // 3. Replace the last user message with the enriched prompt (for the API)
        val lastIndex = history.indexOfLast { it.role == "user" }
        if (lastIndex >= 0) {
            history[lastIndex] = ChatMessage(role = "user", content = prompt)
        }

        // 4. Build the request with the ENTIRE history
        val request = ChatRequest(model = MODEL, messages = history)

        // 4. Call the Groq API with retry logic
        var lastException: Throwable? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = apiService.generateContent("Bearer $apiKey", request)
                val rawText = response.choices
                    ?.firstOrNull()
                    ?.message
                    ?.content
                    ?: throw IllegalStateException("Empty response from Groq API")

                // 5. Insert the assistant's raw response BEFORE parsing
                dao.insertMessage(
                    ChatMessageEntity(role = "assistant", content = rawText)
                )

                // 6. Parse into AgentResponse
                return@runCatching parseAgentResponse(rawText)
            } catch (e: HttpException) {
                if (e.code() == 429 && attempt < MAX_RETRIES - 1) {
                    lastException = e
                    delay(INITIAL_DELAY_MS * (attempt + 1))
                } else throw e
            }
        }
        throw lastException ?: IllegalStateException("Retry exhausted")
    }

    /**
     * Parses the raw LLM text into an [AgentResponse].
     * Strips markdown code fences if present, then tries JSON deserialization.
     * Falls back to wrapping raw text into [AgentResponse.patientMessage].
     */
    private fun parseAgentResponse(raw: String): AgentResponse {
        val cleaned = raw
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            json.decodeFromString<AgentResponse>(cleaned)
        } catch (_: Exception) {
            AgentResponse(patientMessage = raw)
        }
    }
}
