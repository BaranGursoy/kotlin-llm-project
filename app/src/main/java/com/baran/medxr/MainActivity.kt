package com.baran.medxr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.baran.medxr.data.AppDatabase
import com.baran.medxr.network.RetrofitClient
import com.baran.medxr.repository.LlmRepositoryImpl
import com.baran.medxr.ui.ChatScreen
import com.baran.medxr.ui.ChatViewModel
import com.baran.medxr.ui.theme.MedXrCompanionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Compose-observable text field state — updated by both typing and STT. */
    private var inputText by mutableStateOf("")

    private lateinit var speechRecognizer: SpeechRecognizer

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening() else {
                Toast.makeText(this, "Microphone permission is required for STT", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Manual dependency wiring (no DI framework) ─────────────────

        // 1. Room Database
        val database = AppDatabase.getInstance(this)
        val dao = database.chatMessageDao()

        // 2. Repository
        val repository = LlmRepositoryImpl(
            apiService = RetrofitClient.geminiApiService,
            apiKey = BuildConfig.GEMINI_API_KEY,
            dao = dao
        )

        // 3. Seed the system prompt on first launch
        lifecycleScope.launch {
            repository.seedIfEmpty()
        }

        // 4. ViewModel (takes both repository AND dao)
        val viewModel = ViewModelProvider(
            this,
            ChatViewModel.factory(repository, dao)
        )[ChatViewModel::class.java]

        // ── Speech Recognizer setup ────────────────────────────────────
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    inputText = matches[0]
                }
            }

            override fun onError(error: Int) {
                Toast.makeText(
                    this@MainActivity,
                    "Speech recognition error (code $error)",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Required overrides — no-op
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        setContent {
            MedXrCompanionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        viewModel = viewModel,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        onMicClick = { onMicClicked() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun onMicClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}