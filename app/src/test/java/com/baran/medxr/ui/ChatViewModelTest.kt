package com.baran.medxr.ui

import com.baran.medxr.model.AgentResponse
import com.baran.medxr.model.ChatEntry
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FakeLlmRepository(
        private val result: Result<AgentResponse>
    ) : LlmRepository {
        var lastPrompt: String? = null
            private set

        override suspend fun ask(prompt: String): Result<AgentResponse> {
            lastPrompt = prompt
            return result
        }
    }

    private val sampleResponse = AgentResponse(
        patientMessage = "Test response",
        xrSceneRecommendation = "Cardiology",
        urgencyLevel = "Low",
        avatarEmotionTrigger = "calming"
    )

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    fun `initial state is Idle with empty history`() {
        val vm = ChatViewModel(FakeLlmRepository(Result.success(sampleResponse)))
        assertEquals(UiState.Idle, vm.uiState.value)
        assertTrue(vm.chatHistory.value.isEmpty())
    }

    @Test
    fun `sendMessage appends user and assistant messages to history`() = runTest {
        val vm = ChatViewModel(FakeLlmRepository(Result.success(sampleResponse)))

        vm.sendMessage("Hello")
        advanceUntilIdle()

        val history = vm.chatHistory.value
        assertEquals(2, history.size)
        assertTrue(history[0] is ChatEntry.UserMessage)
        assertEquals("Hello", (history[0] as ChatEntry.UserMessage).text)
        assertTrue(history[1] is ChatEntry.AssistantMessage)
        assertEquals(sampleResponse, (history[1] as ChatEntry.AssistantMessage).agentResponse)
        assertEquals(UiState.Idle, vm.uiState.value)
    }

    @Test
    fun `sendMessage keeps user message but sets Error on failure`() = runTest {
        val vm = ChatViewModel(
            FakeLlmRepository(Result.failure(RuntimeException("Network error")))
        )

        vm.sendMessage("Hello")
        advanceUntilIdle()

        val history = vm.chatHistory.value
        assertEquals(1, history.size) // only user message
        assertTrue(vm.uiState.value is UiState.Error)
    }

    @Test
    fun `blank prompt is ignored`() = runTest {
        val vm = ChatViewModel(FakeLlmRepository(Result.success(sampleResponse)))

        vm.sendMessage("   ")
        advanceUntilIdle()

        assertTrue(vm.chatHistory.value.isEmpty())
        assertEquals(UiState.Idle, vm.uiState.value)
    }

    @Test
    fun `wearable toggle prepends sensor data`() = runTest {
        val repo = FakeLlmRepository(Result.success(sampleResponse))
        val vm = ChatViewModel(repo)

        vm.toggleWearable(true)
        vm.sendMessage("chest pain")
        advanceUntilIdle()

        assertTrue(repo.lastPrompt!!.startsWith("[WEARABLE DATA:"))
        assertTrue(repo.lastPrompt!!.contains("chest pain"))
    }

    @Test
    fun `wearable toggle off sends raw prompt`() = runTest {
        val repo = FakeLlmRepository(Result.success(sampleResponse))
        val vm = ChatViewModel(repo)

        vm.toggleWearable(false)
        vm.sendMessage("chest pain")
        advanceUntilIdle()

        assertEquals("chest pain", repo.lastPrompt)
    }

    @Test
    fun `multiple messages build conversation history`() = runTest {
        val vm = ChatViewModel(FakeLlmRepository(Result.success(sampleResponse)))

        vm.sendMessage("First")
        advanceUntilIdle()
        vm.sendMessage("Second")
        advanceUntilIdle()

        val history = vm.chatHistory.value
        assertEquals(4, history.size) // 2 user + 2 assistant
    }
}
