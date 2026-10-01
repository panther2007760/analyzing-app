package com.example.data.forex

import android.util.Log
import com.example.network.ApiClient
import com.example.network.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

data class ForexPair(
    val symbol: String, // e.g. "EUR/USD"
    val base: String,
    val quote: String,
    val rate: Double = 0.0,
    val changePercent: Double = 0.0,
    val isConfiguredForLiveStreaming: Boolean = false,
    val lastUpdate: String = ""
)

class ForexRepository(private val networkMonitor: NetworkMonitor) {

    private val tag = "AivoraForexRepo"

    private val _pairs = MutableStateFlow(
        listOf(
            ForexPair("EUR/USD", "EUR", "USD"),
            ForexPair("GBP/USD", "GBP", "USD"),
            ForexPair("USD/JPY", "USD", "JPY"),
            ForexPair("USD/CHF", "USD", "CHF"),
            ForexPair("AUD/USD", "AUD", "USD"),
            ForexPair("USD/CAD", "USD", "CAD"),
            ForexPair("NZD/USD", "NZD", "USD")
        )
    )
    val pairs: StateFlow<List<ForexPair>> = _pairs.asStateFlow()

    private val _providerStatus = MutableStateFlow("ECB Daily Rates Active (Real-Time Tick Streaming: NOT CONFIGURED)")
    val providerStatus: StateFlow<String> = _providerStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    suspend fun refreshForexRates() {
        if (!networkMonitor.networkStatus.value.isConnected) {
            _errorMessage.value = "OFFLINE: Connect to Internet to fetch latest Forex rates."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        withContext(Dispatchers.IO) {
            try {
                // Free, open, legal public European Central Bank rates endpoint
                val url = "https://api.frankfurter.app/latest?from=USD"
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "AivoraChartAI/1.0")

                val stream = connection.getInputStream()
                val text = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val rates = json.getJSONObject("rates")
                val date = json.optString("date", "Recent")

                val updatedList = ArrayList<ForexPair>()

                for (pair in _pairs.value) {
                    val rate = when (pair.symbol) {
                        "EUR/USD" -> {
                            val eur = rates.optDouble("EUR", 0.0)
                            if (eur > 0.0) 1.0 / eur else 0.0
                        }
                        "GBP/USD" -> {
                            val gbp = rates.optDouble("GBP", 0.0)
                            if (gbp > 0.0) 1.0 / gbp else 0.0
                        }
                        "AUD/USD" -> {
                            val aud = rates.optDouble("AUD", 0.0)
                            if (aud > 0.0) 1.0 / aud else 0.0
                        }
                        "NZD/USD" -> {
                            val nzd = rates.optDouble("NZD", 0.0)
                            if (nzd > 0.0) 1.0 / nzd else 0.0
                        }
                        "USD/JPY" -> rates.optDouble("JPY", 0.0)
                        "USD/CHF" -> rates.optDouble("CHF", 0.0)
                        "USD/CAD" -> rates.optDouble("CAD", 0.0)
                        else -> 0.0
                    }

                    updatedList.add(
                        pair.copy(
                            rate = rate,
                            lastUpdate = date,
                            isConfiguredForLiveStreaming = false // Explicitly NOT CONFIGURED for second-by-second streaming
                        )
                    )
                }

                _pairs.value = updatedList
                _providerStatus.value = "ECB Official Rates ($date). Tick-by-tick streaming: NOT CONFIGURED (Requires OANDA / AlphaVantage API key)."
            } catch (e: Exception) {
                Log.w(tag, "Forex rates refresh failed: ${e.message}")
                _errorMessage.value = "Forex rates unavailable: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
