package com.baran.medxr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baran.medxr.model.ChatEntry
import com.baran.medxr.repository.LlmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat screen.
 *
 * Maintains a persistent [chatHistory] and a transient [uiState] for
 * the current operation (loading spinner, error banner, etc.).
 */
class ChatViewModel(
    private val repository: LlmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Full conversation history, visible in the scrollable chat list. */
    private val _chatHistory = MutableStateFlow<List<ChatEntry>>(emptyList())
    val chatHistory: StateFlow<List<ChatEntry>> = _chatHistory.asStateFlow()

    /** Whether mock wearable data should be prepended to prompts. */
    private val _wearableEnabled = MutableStateFlow(false)
    val wearableEnabled: StateFlow<Boolean> = _wearableEnabled.asStateFlow()

    fun toggleWearable(enabled: Boolean) {
        _wearableEnabled.value = enabled
    }

    /**
     * Sends [prompt] to the LLM, appends the user's message to history
     * immediately, then appends the assistant's response when it arrives.
     */
    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        // Append user message to history immediately
        _chatHistory.value = _chatHistory.value + ChatEntry.UserMessage(prompt)

        val enrichedPrompt = if (_wearableEnabled.value) {
            WEARABLE_PREFIX + prompt
        } else {
            prompt
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.ask(enrichedPrompt)
                .onSuccess { agentResponse ->
                    _chatHistory.value = _chatHistory.value +
                        ChatEntry.AssistantMessage(agentResponse)
                    _uiState.value = UiState.Idle
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(
                        error.localizedMessage ?: "An unknown error occurred"
                    )
                }
        }
    }

    companion object {
        private const val WEARABLE_PREFIX =
            "[WEARABLE DATA: Heart Rate 115 BPM, SpO2 96%, Status: Elevated] User Message: "

        fun factory(repository: LlmRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return ChatViewModel(repository) as T
                }
            }
    }
}
