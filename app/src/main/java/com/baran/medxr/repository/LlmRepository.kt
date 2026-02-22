package com.baran.medxr.repository

import com.baran.medxr.model.AgentResponse

/**
 * Abstraction over the LLM data source.
 *
 * Returns [Result] so callers never deal with raw exceptions —
 * errors are modelled as values.
 */
interface LlmRepository {
    suspend fun ask(prompt: String): Result<AgentResponse>
}
