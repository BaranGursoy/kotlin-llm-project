package com.baran.medxr.repository

import com.baran.medxr.network.ChatMessage
import com.baran.medxr.network.ChatRequest
import com.baran.medxr.network.GeminiApiService
import kotlinx.coroutines.delay
import retrofit2.HttpException

/**
 * Production implementation of [LlmRepository].
 *
 * @param apiService  Retrofit service — constructor-injected for testability.
 * @param apiKey      Groq API key — constructor-injected so it can be swapped.
 */
class LlmRepositoryImpl(
    private val apiService: GeminiApiService,
    private val apiKey: String
) : LlmRepository {

    companion object {
        private const val MODEL = "llama-3.3-70b-versatile"
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1_000L
       private const val SYSTEM_PROMPT = """
        You are the mobile triage agent for a Medical XR Simulation system. 
        Your goal is to converse with the user, assess their symptoms, and prepare a data payload for their upcoming VR avatar consultation.
    
        You MUST respond strictly in the following JSON format:
        {
            "patient_message": "Your empathetic, clear response to the user. Remind them this is for informational purposes.",
            "xr_scene_recommendation": "Identify the relevant medical department (e.g., 'Cardiology', 'Neurology', 'General').",
            "urgency_level": "Low", "Medium", or "High",
            "avatar_emotion_trigger": "Suggest an emotion for the VR avatar based on the user's stress level (e.g., 'calming', 'attentive', 'urgent')."
        }
    """
    }

    override suspend fun ask(prompt: String): Result<String> = runCatching {
        val request = ChatRequest(
            model = MODEL,
            messages = listOf(
                ChatMessage(role = "system", content = SYSTEM_PROMPT),
                ChatMessage(role = "user", content = prompt)
            )
        )

        var lastException: Throwable? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = apiService.generateContent("Bearer $apiKey", request)
                val text = response.choices
                    ?.firstOrNull()
                    ?.message
                    ?.content

                return@runCatching text
                    ?: throw IllegalStateException("Empty response from Groq API")
            } catch (e: HttpException) {
                if (e.code() == 429 && attempt < MAX_RETRIES - 1) {
                    lastException = e
                    delay(INITIAL_DELAY_MS * (attempt + 1))
                } else throw e
            }
        }
        throw lastException ?: IllegalStateException("Retry exhausted")
    }
}
