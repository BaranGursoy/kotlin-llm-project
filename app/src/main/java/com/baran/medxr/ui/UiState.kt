package com.baran.medxr.ui

/**
 * Sealed interface modelling every possible screen state.
 *
 * Using a sealed interface (rather than a sealed class) is idiomatic
 * Kotlin — it allows both `data object` singletons and `data class`
 * variants, and the compiler can verify exhaustive `when` branches.
 */
sealed interface UiState {

    /** No request has been made yet. */
    data object Idle : UiState

    /** A request is in-flight. */
    data object Loading : UiState

    /** The LLM returned a successful response. */
    data class Success(val response: String) : UiState

    /** Something went wrong. */
    data class Error(val message: String) : UiState
}
