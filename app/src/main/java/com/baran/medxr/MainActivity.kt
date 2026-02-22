package com.baran.medxr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.baran.medxr.network.RetrofitClient
import com.baran.medxr.repository.LlmRepositoryImpl
import com.baran.medxr.ui.ChatScreen
import com.baran.medxr.ui.ChatViewModel
import com.baran.medxr.ui.theme.MedXrCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Manual dependency wiring (no DI framework) ─────────────────
        val repository = LlmRepositoryImpl(
            apiService = RetrofitClient.geminiApiService,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
        val viewModel = ViewModelProvider(
            this,
            ChatViewModel.factory(repository)
        )[ChatViewModel::class.java]

        setContent {
            MedXrCompanionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}