package com.baran.medxr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baran.medxr.repository.LlmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat screen.
 *
 * The [repository] is **constructor-injected** — no service locator or
 * static singleton — making this class trivially testable.
 */
class ChatViewModel(
    private val repository: LlmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)

    /** Observable UI state consumed by the Compose layer. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Sends [prompt] to the LLM and updates [uiState] through the
     * Loading → Success / Error lifecycle.
     */
    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.ask(prompt)
                .onSuccess { response -> _uiState.value = UiState.Success(response) }
                .onFailure { error ->
                    _uiState.value = UiState.Error(
                        error.localizedMessage ?: "An unknown error occurred"
                    )
                }
        }
    }

    /**
     * [ViewModelProvider.Factory] to provide [ChatViewModel] with its
     * constructor dependency without a DI framework.
     */
    companion object {
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
