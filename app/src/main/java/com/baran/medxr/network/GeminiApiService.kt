package com.baran.medxr.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service interface targeting the Groq chat/completions endpoint
 * (OpenAI-compatible format).
 */
interface GeminiApiService {

    @POST("chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): ChatResponse
}
