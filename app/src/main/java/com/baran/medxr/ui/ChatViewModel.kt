package com.baran.medxr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baran.medxr.data.ChatMessageEntity
import androidx.lifecycle.viewModelScope
import com.baran.medxr.data.ChatMessageDao
import com.baran.medxr.model.AgentResponse
import com.baran.medxr.model.ChatEntry
import com.baran.medxr.repository.LlmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * ViewModel for the chat screen.
 *
 * Observes the Room database via [ChatMessageDao.observeAll] as the
 * **single source of truth** for conversation history.
 */
class ChatViewModel(
    private val repository: LlmRepository,
    private val dao: ChatMessageDao
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Chat history observed reactively from Room.
     * Automatically recomposes the UI when the database changes.
     * System-role messages are filtered out — only user/assistant shown.
     */
    val chatHistory: StateFlow<List<ChatEntry>> = dao.observeAll()
        .map { entities ->
            entities
                .filter { it.role != "system" }
                .map { entity ->
                    when (entity.role) {
                        "user" -> ChatEntry.UserMessage(entity.content)
                        "assistant" -> {
                            val agentResponse = parseAgentResponse(entity.content)
                            ChatEntry.AssistantMessage(agentResponse)
                        }
                        else -> ChatEntry.UserMessage(entity.content)
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Whether mock wearable data should be prepended to prompts. */
    private val _wearableEnabled = MutableStateFlow(false)
    val wearableEnabled: StateFlow<Boolean> = _wearableEnabled.asStateFlow()

    fun toggleWearable(enabled: Boolean) {
        _wearableEnabled.value = enabled
    }

    /** Clears the entire conversation and re-seeds the system prompt. */
    fun clearChat() {
        viewModelScope.launch {
            dao.deleteAll()
            dao.insertMessage(
                ChatMessageEntity(
                    role = "system",
                    content = com.baran.medxr.repository.LlmRepositoryImpl.SYSTEM_PROMPT.trimIndent()
                )
            )
            _uiState.value = UiState.Idle
        }
    }

    /**
     * Sends [prompt] to the LLM. The repository handles inserting
     * both user and assistant messages into Room — the UI updates
     * automatically via the [chatHistory] Flow.
     */
    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        val enrichedPrompt = if (_wearableEnabled.value) {
            WEARABLE_PREFIX + prompt
        } else {
            prompt
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.ask(enrichedPrompt, prompt)
                .onSuccess { _uiState.value = UiState.Idle }
                .onFailure { error ->
                    _uiState.value = UiState.Error(
                        error.localizedMessage ?: "An unknown error occurred"
                    )
                }
        }
    }

    /**
     * Parses raw assistant content into [AgentResponse].
     * Mirrors the same logic in [LlmRepositoryImpl].
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

    companion object {
        private const val WEARABLE_PREFIX =
            "[WEARABLE DATA: Heart Rate 115 BPM, SpO2 96%, Status: Elevated] User Message: "

        fun factory(repository: LlmRepository, dao: ChatMessageDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return ChatViewModel(repository, dao) as T
                }
            }
    }
}
