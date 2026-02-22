package com.baran.medxr.repository

import com.baran.medxr.model.AgentResponse

/**
 * Abstraction over the LLM data source.
 *
 * Returns [Result] so callers never deal with raw exceptions —
 * errors are modelled as values.
 */
interface LlmRepository {
    /**
     * @param prompt         The full prompt sent to the LLM (may include wearable prefix).
     * @param displayPrompt  The clean user text stored in Room for display.
     */
    suspend fun ask(prompt: String, displayPrompt: String): Result<AgentResponse>
}
