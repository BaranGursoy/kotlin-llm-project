package com.baran.medxr.ui

import com.baran.medxr.model.AgentResponse

/**
 * Sealed interface modelling the transient operation state.
 *
 * The conversation history is maintained separately in [ChatViewModel.chatHistory].
 * This state only tracks whether we are idle, loading, or in an error state.
 */
sealed interface UiState {

    /** Ready for input / conversation flowing normally. */
    data object Idle : UiState

    /** A request is in-flight. */
    data object Loading : UiState

    /** Something went wrong with the latest request. */
    data class Error(val message: String) : UiState
}
