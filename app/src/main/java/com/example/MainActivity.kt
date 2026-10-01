package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.MarketRepository
import com.example.ui.AivoraApp
import com.example.ui.theme.AivoraTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: MarketRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = MarketRepository(applicationContext)

        setContent {
            AivoraTheme {
                AivoraApp(
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // When returning from background, re-verify network state and refresh live streams
        repository.retryConnection()
    }
}
