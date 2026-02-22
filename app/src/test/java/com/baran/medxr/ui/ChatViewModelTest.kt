package com.baran.medxr.ui

import com.baran.medxr.repository.LlmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChatViewModel].
 *
 * Uses a [FakeLlmRepository] to verify state transitions
 * without hitting the real network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private class FakeLlmRepository(
        private val result: Result<String>
    ) : LlmRepository {
        override suspend fun ask(prompt: String): Result<String> = result
    }

    private fun createViewModel(result: Result<String>): ChatViewModel {
        return ChatViewModel(FakeLlmRepository(result))
    }

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    fun `initial state is Idle`() {
        val viewModel = createViewModel(Result.success(""))
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `sendMessage ends in Success on successful call`() = runTest {
        val expected = "Test response"
        val viewModel = createViewModel(Result.success(expected))

        viewModel.sendMessage("Hello")

        // Allow the coroutine to complete
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success but got $state", state is UiState.Success)
        assertEquals(expected, (state as UiState.Success).response)
    }

    @Test
    fun `sendMessage ends in Error on failure`() = runTest {
        val errorMessage = "Network error"
        val viewModel = createViewModel(Result.failure(RuntimeException(errorMessage)))

        viewModel.sendMessage("Hello")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is UiState.Error)
        assertEquals(errorMessage, (state as UiState.Error).message)
    }

    @Test
    fun `sendMessage with blank prompt is ignored`() = runTest {
        val viewModel = createViewModel(Result.success("response"))

        viewModel.sendMessage("   ")
        advanceUntilIdle()

        // Should remain Idle — blank prompts are no-ops
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }
}
