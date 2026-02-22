package com.baran.medxr.model

import com.baran.medxr.model.AgentResponse

/**
 * Represents a single message in the chat history.
 */
sealed interface ChatEntry {

    /** A message sent by the user. */
    data class UserMessage(val text: String) : ChatEntry

    /** A structured response from the LLM agent. */
    data class AssistantMessage(val agentResponse: AgentResponse) : ChatEntry
}
