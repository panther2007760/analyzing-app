package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.db.AivoraDatabase
import com.example.data.db.PriceAlertEntity
import com.example.data.db.SignalEntity
import com.example.data.engine.SignalEngine
import com.example.data.engine.TechnicalSignal
import com.example.data.forex.ForexRepository
import com.example.model.AIAnalysis
import com.example.model.AppLanguage
import com.example.model.CandleStick
import com.example.model.CryptoPair
import com.example.model.DataSourceMode
import com.example.model.DebugMetrics
import com.example.model.MarketDataStatus
import com.example.model.NetworkStatus
import com.example.model.Ticker
import com.example.model.Timeframe
import com.example.model.WebSocketConnectionState
import com.example.network.ApiClient
import com.example.network.GeminiApiService
import com.example.network.MarketApiService
import com.example.network.MarketWebSocketManager
import com.example.network.NetworkMonitor
import com.example.service.AlertNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLException
import kotlin.math.max
import kotlin.math.min

class MarketRepository(context: Context) {

    private val tag = "AivoraMarketRepo"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val networkMonitor = NetworkMonitor(context.applicationContext)
    val webSocketManager = MarketWebSocketManager(networkMonitor, scope)
    val geminiApiService = GeminiApiService()

    // Database & Notifications
    private val database = AivoraDatabase.getDatabase(context.applicationContext)
    val signalDao = database.signalDao()
    val priceAlertDao = database.priceAlertDao()
    val alertNotificationManager = AlertNotificationManager(context.applicationContext, priceAlertDao, scope)
    val forexRepository = ForexRepository(networkMonitor)

    // UI Configuration State
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private var marketApiService: MarketApiService = ApiClient.getMarketService(ApiClient.DEFAULT_COINBASE_BASE_URL)

    private val _selectedPair = MutableStateFlow(CryptoPair.DEFAULT_PAIRS.first())
    val selectedPair: StateFlow<CryptoPair> = _selectedPair.asStateFlow()

    private val _currentTimeframe = MutableStateFlow(Timeframe.H1)
    val currentTimeframe: StateFlow<Timeframe> = _currentTimeframe.asStateFlow()

    private val _dataSourceMode = MutableStateFlow(DataSourceMode.LIVE)
    val dataSourceMode: StateFlow<DataSourceMode> = _dataSourceMode.asStateFlow()

    private val _marketDataStatus = MutableStateFlow(MarketDataStatus.CONNECTING)
    val marketDataStatus: StateFlow<MarketDataStatus> = _marketDataStatus.asStateFlow()

    private val _lastDataUpdateTime = MutableStateFlow(0L)
    val lastDataUpdateTime: StateFlow<Long> = _lastDataUpdateTime.asStateFlow()

    private val _lastAnalysisTime = MutableStateFlow(0L)
    val lastAnalysisTime: StateFlow<Long> = _lastAnalysisTime.asStateFlow()

    private val _currentTicker = MutableStateFlow<Ticker?>(null)
    val currentTicker: StateFlow<Ticker?> = _currentTicker.asStateFlow()

    private val _candles = MutableStateFlow<List<CandleStick>>(emptyList())
    val candles: StateFlow<List<CandleStick>> = _candles.asStateFlow()

    private val _htfCandles = MutableStateFlow<List<CandleStick>>(emptyList())

    private val _currentSignal = MutableStateFlow<TechnicalSignal?>(null)
    val currentSignal: StateFlow<TechnicalSignal?> = _currentSignal.asStateFlow()

    private val _scannerSignals = MutableStateFlow<List<TechnicalSignal>>(emptyList())
    val scannerSignals: StateFlow<List<TechnicalSignal>> = _scannerSignals.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isLoadingMarketData = MutableStateFlow(false)
    val isLoadingMarketData: StateFlow<Boolean> = _isLoadingMarketData.asStateFlow()

    private val _marketErrorMessage = MutableStateFlow<String?>(null)
    val marketErrorMessage: StateFlow<String?> = _marketErrorMessage.asStateFlow()

    private val _liveAiAnalysis = MutableStateFlow<AIAnalysis?>(null)
    val liveAiAnalysis: StateFlow<AIAnalysis?> = _liveAiAnalysis.asStateFlow()

    private val _isLiveAiLoading = MutableStateFlow(false)
    val isLiveAiLoading: StateFlow<Boolean> = _isLiveAiLoading.asStateFlow()

    private val _screenshotAiAnalysis = MutableStateFlow<AIAnalysis?>(null)
    val screenshotAiAnalysis: StateFlow<AIAnalysis?> = _screenshotAiAnalysis.asStateFlow()

    private val _isScreenshotAiLoading = MutableStateFlow(false)
    val isScreenshotAiLoading: StateFlow<Boolean> = _isScreenshotAiLoading.asStateFlow()

    val savedSignalsFlow: Flow<List<SignalEntity>> = signalDao.getSavedSignals()
    val activeAlertsFlow: Flow<List<PriceAlertEntity>> = priceAlertDao.getActiveAlerts()
    val allAlertsFlow: Flow<List<PriceAlertEntity>> = priceAlertDao.getAllAlerts()

    private val _debugMetrics = MutableStateFlow(
        DebugMetrics(
            feedProvider = "Coinbase Exchange (Global)",
            apiBaseUrl = ApiClient.DEFAULT_COINBASE_BASE_URL,
            webSocketUrl = ApiClient.DEFAULT_COINBASE_WS_URL
        )
    )
    val debugMetrics: StateFlow<DebugMetrics> = _debugMetrics.asStateFlow()

    private var staleCheckJob: Job? = null

    init {
        // Observe network state: auto-refresh when reconnected
        scope.launch {
            networkMonitor.networkStatus.collect { status ->
                updateDebugNetwork(status)
                if (status.isConnected) {
                    if (_dataSourceMode.value == DataSourceMode.LIVE) {
                        if (_candles.value.isEmpty() || _marketErrorMessage.value != null) {
                            Log.i(tag, "Network available, auto-fetching market data...")
                            loadMarketData(_selectedPair.value, _currentTimeframe.value)
                        }
                    }
                } else {
                    _marketDataStatus.value = MarketDataStatus.OFFLINE
                }
            }
        }

        // Listen for live WebSocket ticker updates and perform real-time candle aggregation
        scope.launch {
            webSocketManager.tickerFlow.collect { tick ->
                val current = _selectedPair.value
                val matches = tick.symbol.equals(current.coinbaseProduct, ignoreCase = true) ||
                        tick.symbol.equals(current.binanceSymbol, ignoreCase = true) ||
                        tick.symbol.equals(current.symbol, ignoreCase = true) ||
                        tick.symbol.startsWith(current.baseAsset, ignoreCase = true)

                if (_dataSourceMode.value == DataSourceMode.LIVE && matches) {
                    _currentTicker.value = tick
                    _marketErrorMessage.value = null
                    _lastDataUpdateTime.value = System.currentTimeMillis()
                    _marketDataStatus.value = MarketDataStatus.LIVE

                    // Update live candlestick dynamically
                    updateLiveCandleFromTick(tick.lastPrice, tick.timestamp)

                    // Re-calculate technical signal with latest tick and updated candles
                    updateCurrentSignal(current, _currentTimeframe.value, _candles.value, tick.lastPrice)

                    // Check if price triggers any active price alert
                    alertNotificationManager.checkAlertsForPrice(current.coinbaseProduct, tick.lastPrice)
                }
            }
        }

        // Keep debug metrics updated from WebSocket manager
        scope.launch {
            webSocketManager.connectionState.collect { state ->
                _debugMetrics.value = _debugMetrics.value.copy(
                    webSocketState = state,
                    feedProvider = "${webSocketManager.currentProvider.value.name} (Global)",
                    webSocketUrl = webSocketManager.getWebSocketUrl()
                )
                if (state == WebSocketConnectionState.CONNECTED) {
                    _marketDataStatus.value = MarketDataStatus.LIVE
                } else if (state == WebSocketConnectionState.CONNECTING) {
                    if (_marketDataStatus.value != MarketDataStatus.LIVE) {
                        _marketDataStatus.value = MarketDataStatus.CONNECTING
                    }
                } else if (state == WebSocketConnectionState.OFFLINE_PAUSED) {
                    _marketDataStatus.value = MarketDataStatus.OFFLINE
                }
            }
        }

        scope.launch {
            webSocketManager.lastMessageTime.collect { time ->
                _debugMetrics.value = _debugMetrics.value.copy(
                    webSocketMessagesCount = webSocketManager.messageCount.get(),
                    lastWebSocketMessageTime = time
                )
            }
        }

        scope.launch {
            webSocketManager.lastError.collect { err ->
                if (err != null) {
                    recordError("WebSocket: $err")
                }
            }
        }

        // Stale Data Protection Watchdog Loop
        startStaleDataWatchdog()

        // Initial load
        selectPair(_selectedPair.value)
        scope.launch {
            forexRepository.refreshForexRates()
        }
    }

    private fun startStaleDataWatchdog() {
        staleCheckJob?.cancel()
        staleCheckJob = scope.launch(Dispatchers.Default) {
            while (true) {
                delay(12000L)
                if (_dataSourceMode.value == DataSourceMode.LIVE && networkMonitor.networkStatus.value.isConnected) {
                    val lastUpdate = _lastDataUpdateTime.value
                    if (lastUpdate > 0L) {
                        val elapsed = System.currentTimeMillis() - lastUpdate
                        val staleThreshold = when (_currentTimeframe.value) {
                            Timeframe.M1 -> 45_000L
                            Timeframe.M5 -> 120_000L
                            Timeframe.M15 -> 180_000L
                            else -> 300_000L
                        }

                        if (elapsed > staleThreshold && _marketDataStatus.value == MarketDataStatus.LIVE) {
                            Log.w(tag, "Stale data detected (${elapsed}ms without update). Marking STALE.")
                            _marketDataStatus.value = MarketDataStatus.STALE
                            // Fallback refresh to restore fresh data
                            loadMarketData(_selectedPair.value, _currentTimeframe.value)
                        }
                    }
                }
            }
        }
    }

    private fun updateLiveCandleFromTick(price: Double, tickTime: Long) {
        if (price <= 0.0) return
        val currentList = _candles.value
        if (currentList.isEmpty()) return

        val duration = _currentTimeframe.value.durationMs
        val lastCandle = currentList.last()

        val isNewCandle = tickTime >= lastCandle.openTime + duration

        if (isNewCandle) {
            val newOpenTime = lastCandle.openTime + duration
            val newCandle = CandleStick(
                openTime = newOpenTime,
                open = price,
                high = price,
                low = price,
                close = price,
                volume = 0.1,
                closeTime = newOpenTime + duration - 1L
            )
            val updated = ArrayList(currentList)
            if (updated.size >= 50) updated.removeAt(0)
            updated.add(newCandle)
            _candles.value = updated
        } else {
            val updatedCandle = lastCandle.copy(
                high = max(lastCandle.high, price),
                low = min(lastCandle.low, price),
                close = price,
                volume = lastCandle.volume + 0.05
            )
            val updated = ArrayList(currentList)
            updated[updated.size - 1] = updatedCandle
            _candles.value = updated
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setTimeframe(tf: Timeframe) {
        if (_currentTimeframe.value != tf) {
            _currentTimeframe.value = tf
            loadMarketData(_selectedPair.value, tf)
        }
    }

    fun selectPair(pair: CryptoPair) {
        _selectedPair.value = pair
        _liveAiAnalysis.value = null
        if (_dataSourceMode.value == DataSourceMode.LIVE) {
            webSocketManager.subscribePair(pair)
            loadMarketData(pair, _currentTimeframe.value)
        } else {
            loadDemoData(pair)
        }
    }

    fun toggleDataSourceMode(mode: DataSourceMode) {
        _dataSourceMode.value = mode
        if (mode == DataSourceMode.LIVE) {
            webSocketManager.subscribePair(_selectedPair.value)
            loadMarketData(_selectedPair.value, _currentTimeframe.value)
        } else {
            webSocketManager.disconnect()
            loadDemoData(_selectedPair.value)
        }
    }

    fun retryConnection() {
        networkMonitor.updateStatus()
        if (_dataSourceMode.value == DataSourceMode.LIVE) {
            webSocketManager.subscribePair(_selectedPair.value)
            loadMarketData(_selectedPair.value, _currentTimeframe.value)
        }
        scope.launch {
            forexRepository.refreshForexRates()
        }
    }

    fun loadMarketData(
        pair: CryptoPair = _selectedPair.value,
        timeframe: Timeframe = _currentTimeframe.value
    ) {
        scope.launch {
            _isLoadingMarketData.value = true
            val isOnline = networkMonitor.networkStatus.value.isConnected

            if (!isOnline) {
                _isLoadingMarketData.value = false
                _marketDataStatus.value = MarketDataStatus.OFFLINE
                _marketErrorMessage.value = "No Internet connection detected. Check your Wi-Fi or mobile data."
                recordError("Market API: Offline. Cannot load live ticker for ${pair.displayName}.")
                return@launch
            }

            _marketDataStatus.value = MarketDataStatus.CONNECTING

            try {
                val start = System.currentTimeMillis()
                var success = false

                // 1. Fetch Coinbase Candles for selected timeframe
                try {
                    val statsResp = withContext(Dispatchers.IO) {
                        marketApiService.getCoinbaseStats(pair.coinbaseProduct)
                    }
                    val candlesResp = withContext(Dispatchers.IO) {
                        marketApiService.getCoinbaseCandles(
                            productId = pair.coinbaseProduct,
                            granularity = timeframe.coinbaseGranularitySeconds
                        )
                    }

                    // Also fetch 1H / HTF candles for multi-timeframe confirmation
                    if (timeframe != Timeframe.H1) {
                        launch(Dispatchers.IO) {
                            try {
                                val hResp = marketApiService.getCoinbaseCandles(pair.coinbaseProduct, 3600)
                                if (hResp.isSuccessful && hResp.body() != null) {
                                    val parsedH = hResp.body()!!.mapNotNull { row ->
                                        if (row.size >= 6) {
                                            CandleStick(
                                                openTime = row[0].toLong() * 1000L,
                                                low = row[1],
                                                high = row[2],
                                                open = row[3],
                                                close = row[4],
                                                volume = row[5]
                                            )
                                        } else null
                                    }.filter { it.isValid() }.sortedBy { it.openTime }
                                    _htfCandles.value = parsedH
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (statsResp.isSuccessful && statsResp.body() != null) {
                        val stats = statsResp.body()!!
                        val lastPrice = stats.last?.toDoubleOrNull() ?: 0.0
                        val openPrice = stats.open?.toDoubleOrNull() ?: 0.0
                        val highPrice = stats.high?.toDoubleOrNull() ?: 0.0
                        val lowPrice = stats.low?.toDoubleOrNull() ?: 0.0
                        val volume = stats.volume?.toDoubleOrNull() ?: 0.0
                        val priceChange = if (openPrice > 0) lastPrice - openPrice else 0.0
                        val percentChange = if (openPrice > 0) (priceChange / openPrice) * 100.0 else 0.0

                        _currentTicker.value = Ticker(
                            symbol = pair.coinbaseProduct,
                            lastPrice = lastPrice,
                            priceChange24h = priceChange,
                            percentChange24h = percentChange,
                            highPrice24h = highPrice,
                            lowPrice24h = lowPrice,
                            volume24h = volume,
                            timestamp = System.currentTimeMillis(),
                            isLive = true
                        )
                        _marketErrorMessage.value = null
                        _lastDataUpdateTime.value = System.currentTimeMillis()
                        _marketDataStatus.value = MarketDataStatus.LIVE
                        success = true
                    }

                    if (candlesResp.isSuccessful && candlesResp.body() != null) {
                        val rawList = candlesResp.body()!!
                        val parsed = rawList.mapNotNull { row ->
                            try {
                                if (row.size >= 6) {
                                    val timeSec = row[0].toLong()
                                    CandleStick(
                                        openTime = timeSec * 1000L,
                                        low = row[1],
                                        high = row[2],
                                        open = row[3],
                                        close = row[4],
                                        volume = row[5],
                                        closeTime = (timeSec + timeframe.coinbaseGranularitySeconds) * 1000L - 1L
                                    )
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }.filter { it.isValid() }.distinctBy { it.openTime }.sortedBy { it.openTime }.takeLast(45)

                        if (parsed.isNotEmpty()) {
                            _candles.value = parsed
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Coinbase API attempt error: ${e.message}. Trying Binance fallback...")
                }

                // Fallback to Binance if Coinbase was not reachable
                if (!success) {
                    val binanceService = ApiClient.getMarketService(ApiClient.DEFAULT_BINANCE_BASE_URL)
                    val tickerResp = withContext(Dispatchers.IO) {
                        binanceService.get24hTicker(pair.binanceSymbol)
                    }
                    val klinesResp = withContext(Dispatchers.IO) {
                        binanceService.getKlines(
                            symbol = pair.binanceSymbol,
                            interval = timeframe.binanceInterval,
                            limit = 45
                        )
                    }

                    if (tickerResp.isSuccessful && tickerResp.body() != null) {
                        val raw = tickerResp.body()!!
                        _currentTicker.value = Ticker(
                            symbol = raw.symbol,
                            lastPrice = raw.lastPrice?.toDoubleOrNull() ?: 0.0,
                            priceChange24h = raw.priceChange?.toDoubleOrNull() ?: 0.0,
                            percentChange24h = raw.priceChangePercent?.toDoubleOrNull() ?: 0.0,
                            highPrice24h = raw.highPrice?.toDoubleOrNull() ?: 0.0,
                            lowPrice24h = raw.lowPrice?.toDoubleOrNull() ?: 0.0,
                            volume24h = raw.volume?.toDoubleOrNull() ?: 0.0,
                            timestamp = raw.closeTime ?: System.currentTimeMillis(),
                            isLive = true
                        )
                        _marketErrorMessage.value = null
                        _lastDataUpdateTime.value = System.currentTimeMillis()
                        _marketDataStatus.value = MarketDataStatus.LIVE
                        success = true
                    }

                    if (klinesResp.isSuccessful && klinesResp.body() != null) {
                        val parsed = klinesResp.body()!!.mapNotNull { row ->
                            try {
                                if (row.size >= 7) {
                                    CandleStick(
                                        openTime = (row[0] as? Number)?.toLong() ?: 0L,
                                        open = (row[1] as? String)?.toDoubleOrNull() ?: 0.0,
                                        high = (row[2] as? String)?.toDoubleOrNull() ?: 0.0,
                                        low = (row[3] as? String)?.toDoubleOrNull() ?: 0.0,
                                        close = (row[4] as? String)?.toDoubleOrNull() ?: 0.0,
                                        volume = (row[5] as? String)?.toDoubleOrNull() ?: 0.0,
                                        closeTime = (row[6] as? Number)?.toLong() ?: 0L
                                    )
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }.filter { it.isValid() }.distinctBy { it.openTime }.sortedBy { it.openTime }

                        if (parsed.isNotEmpty()) {
                            _candles.value = parsed
                        }
                    }
                }

                val latency = System.currentTimeMillis() - start
                if (success) {
                    _debugMetrics.value = _debugMetrics.value.copy(
                        apiStatus = "200 OK (${latency}ms)",
                        apiLatencyMs = latency,
                        lastApiSuccessTime = System.currentTimeMillis()
                    )
                    val price = _currentTicker.value?.lastPrice ?: 0.0
                    updateCurrentSignal(pair, timeframe, _candles.value, price)
                } else {
                    val err = "Could not fetch ticker from live market feeds."
                    _marketErrorMessage.value = err
                    _marketDataStatus.value = MarketDataStatus.ERROR
                    recordError(err)
                }
            } catch (e: UnknownHostException) {
                val msg = "DNS failure: Unable to resolve market host. Verify connection."
                _marketErrorMessage.value = msg
                _marketDataStatus.value = MarketDataStatus.ERROR
                recordError(msg)
            } catch (e: SocketTimeoutException) {
                val msg = "API connection timed out. Network latency high."
                _marketErrorMessage.value = msg
                _marketDataStatus.value = MarketDataStatus.ERROR
                recordError(msg)
            } catch (e: SSLException) {
                val msg = "SSL handshake failure: ${e.message}"
                _marketErrorMessage.value = msg
                _marketDataStatus.value = MarketDataStatus.ERROR
                recordError(msg)
            } catch (e: Exception) {
                val msg = "Market API error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                _marketErrorMessage.value = msg
                _marketDataStatus.value = MarketDataStatus.ERROR
                recordError(msg)
            } finally {
                _isLoadingMarketData.value = false
            }
        }
    }

    private fun updateCurrentSignal(
        pair: CryptoPair,
        timeframe: Timeframe,
        candles: List<CandleStick>,
        price: Double
    ) {
        val signal = SignalEngine.generateSignal(pair, timeframe, candles, price, _htfCandles.value)
        _currentSignal.value = signal
        _lastAnalysisTime.value = System.currentTimeMillis()
    }

    private fun loadDemoData(pair: CryptoPair) {
        _currentTicker.value = DemoDataProvider.getDemoTicker(pair)
        val demoCandles = DemoDataProvider.getDemoCandles(_currentTicker.value?.lastPrice ?: 50000.0)
        _candles.value = demoCandles
        _marketErrorMessage.value = null
        _marketDataStatus.value = MarketDataStatus.LIVE
        _lastDataUpdateTime.value = System.currentTimeMillis()
        _liveAiAnalysis.value = DemoDataProvider.getDemoAnalysis(pair)
        updateCurrentSignal(pair, _currentTimeframe.value, demoCandles, _currentTicker.value?.lastPrice ?: 50000.0)
    }

    fun saveCurrentSignal() {
        val signal = _currentSignal.value ?: return
        scope.launch(Dispatchers.IO) {
            val entity = SignalEntity(
                pairSymbol = signal.pairSymbol,
                timestamp = signal.timestamp,
                bias = signal.bias.name,
                strength = signal.strength.name,
                currentPrice = signal.currentPrice,
                entryZone = signal.entryZone,
                stopLoss = signal.stopLoss,
                takeProfit1 = signal.takeProfit1,
                takeProfit2 = signal.takeProfit2,
                riskRewardRatio = signal.riskRewardRatio,
                rsi = signal.rsi,
                ema9 = signal.ema9,
                ema21 = signal.ema21,
                reasonsSummary = signal.reasons.joinToString("\n• "),
                invalidation = signal.invalidationCondition,
                isSaved = true
            )
            signalDao.insertSignal(entity)
        }
    }

    fun deleteSignal(id: Long) {
        scope.launch(Dispatchers.IO) {
            signalDao.deleteSignalById(id)
        }
    }

    fun clearAllSavedSignals() {
        scope.launch(Dispatchers.IO) {
            signalDao.clearAllSignals()
        }
    }

    fun createPriceAlert(pairSymbol: String, targetPrice: Double, isAbove: Boolean) {
        scope.launch(Dispatchers.IO) {
            val alert = PriceAlertEntity(
                pairSymbol = pairSymbol,
                targetPrice = targetPrice,
                isAbove = isAbove,
                isActive = true
            )
            priceAlertDao.insertAlert(alert)
        }
    }

    fun deletePriceAlert(id: Long) {
        scope.launch(Dispatchers.IO) {
            priceAlertDao.deleteAlertById(id)
        }
    }

    fun scanAllPairs() {
        scope.launch {
            _isScanning.value = true
            val results = ArrayList<TechnicalSignal>()

            for (pair in CryptoPair.DEFAULT_PAIRS) {
                try {
                    val candlesResp = withContext(Dispatchers.IO) {
                        marketApiService.getCoinbaseCandles(
                            pair.coinbaseProduct,
                            _currentTimeframe.value.coinbaseGranularitySeconds
                        )
                    }
                    if (candlesResp.isSuccessful && candlesResp.body() != null) {
                        val candles = candlesResp.body()!!.mapNotNull { row ->
                            try {
                                if (row.size >= 6) {
                                    val timeSec = row[0].toLong()
                                    CandleStick(
                                        openTime = timeSec * 1000L,
                                        low = row[1],
                                        high = row[2],
                                        open = row[3],
                                        close = row[4],
                                        volume = row[5]
                                    )
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }.filter { it.isValid() }.sortedBy { it.openTime }.takeLast(40)

                        val price = candles.lastOrNull()?.close ?: 0.0
                        val sig = SignalEngine.generateSignal(pair, _currentTimeframe.value, candles, price)
                        results.add(sig)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Scan failed for ${pair.symbol}: ${e.message}")
                }
            }

            _scannerSignals.value = results
            _isScanning.value = false
        }
    }

    suspend fun askAiChat(question: String): Result<String> {
        val pair = _selectedPair.value
        val ticker = _currentTicker.value
        val candles = _candles.value
        val signal = _currentSignal.value
        val summary = signal?.let {
            "${it.bias.name} (${it.strength.name}), RSI: ${String.format(Locale.US, "%.1f", it.rsi)}, EMA9: ${it.ema9}, EMA21: ${it.ema21}, EMA50: ${it.ema50}, MACD Hist: ${it.macdHistogram}, Structure: ${it.marketStructure.structureSummary}, Pattern: ${it.candlestickPattern.description}"
        } ?: "No calculated metrics."

        return geminiApiService.askChatAssistant(question, pair, ticker, candles, summary)
    }

    fun runLiveTechnicalAnalysis() {
        scope.launch {
            if (_dataSourceMode.value == DataSourceMode.DEMO) {
                _liveAiAnalysis.value = DemoDataProvider.getDemoAnalysis(_selectedPair.value)
                return@launch
            }

            _isLiveAiLoading.value = true
            val isOnline = networkMonitor.networkStatus.value.isConnected
            if (!isOnline) {
                _isLiveAiLoading.value = false
                val msg = "Cannot perform AI analysis: Device is offline."
                _liveAiAnalysis.value = null
                recordError(msg)
                return@launch
            }

            val pair = _selectedPair.value
            val ticker = _currentTicker.value
            val candles = _candles.value
            val currentSig = _currentSignal.value

            val result = geminiApiService.analyzeLiveMarket(pair, ticker, candles)
            result.onSuccess { analysis ->
                _liveAiAnalysis.value = analysis
                _debugMetrics.value = _debugMetrics.value.copy(
                    aiApiStatus = "Analysis 200 OK (${System.currentTimeMillis()})"
                )
            }.onFailure { ex ->
                _liveAiAnalysis.value = null
                val err = "AI Analysis Failed: ${ex.message}"
                recordError(err)
                _debugMetrics.value = _debugMetrics.value.copy(
                    aiApiStatus = "Failed: ${ex.localizedMessage}"
                )
            }
            _isLiveAiLoading.value = false
        }
    }

    fun runScreenshotAnalysis(bitmap: Bitmap, contextLabel: String = "Uploaded Chart Screenshot") {
        scope.launch {
            _isScreenshotAiLoading.value = true
            val isOnline = networkMonitor.networkStatus.value.isConnected
            if (!isOnline) {
                _isScreenshotAiLoading.value = false
                recordError("Screenshot AI: Offline. Internet required for Gemini Vision.")
                return@launch
            }

            val result = geminiApiService.analyzeScreenshot(bitmap, contextLabel)
            result.onSuccess { analysis ->
                _screenshotAiAnalysis.value = analysis
            }.onFailure { ex ->
                _screenshotAiAnalysis.value = null
                val err = "Screenshot Analysis Failed: ${ex.message}"
                recordError(err)
            }
            _isScreenshotAiLoading.value = false
        }
    }

    fun testApiConnection() {
        scope.launch {
            _debugMetrics.value = _debugMetrics.value.copy(apiStatus = "Testing...")
            val isOnline = networkMonitor.networkStatus.value.isConnected
            if (!isOnline) {
                _debugMetrics.value = _debugMetrics.value.copy(apiStatus = "Failed: Device is offline")
                recordError("API Test: Device is offline.")
                return@launch
            }

            try {
                val start = System.currentTimeMillis()
                val resp = withContext(Dispatchers.IO) { marketApiService.getCoinbaseTime() }
                val latency = System.currentTimeMillis() - start
                if (resp.isSuccessful) {
                    _debugMetrics.value = _debugMetrics.value.copy(
                        apiStatus = "Coinbase Time OK (${latency}ms)",
                        apiLatencyMs = latency,
                        lastApiSuccessTime = System.currentTimeMillis()
                    )
                } else {
                    _debugMetrics.value = _debugMetrics.value.copy(apiStatus = "HTTP ${resp.code()} Error")
                    recordError("API Test HTTP ${resp.code()}")
                }
            } catch (e: Exception) {
                _debugMetrics.value = _debugMetrics.value.copy(apiStatus = "Failed: ${e.localizedMessage}")
                recordError("API Test Error: ${e.message}")
            }
        }
    }

    fun testWebSocket() {
        webSocketManager.forceReconnect()
    }

    fun testAiConnection() {
        scope.launch {
            _debugMetrics.value = _debugMetrics.value.copy(aiApiStatus = "Testing AI Key...")
            val isOnline = networkMonitor.networkStatus.value.isConnected
            if (!isOnline) {
                _debugMetrics.value = _debugMetrics.value.copy(aiApiStatus = "Offline: Check connection")
                return@launch
            }

            val res = geminiApiService.testConnection()
            res.onSuccess { msg ->
                _debugMetrics.value = _debugMetrics.value.copy(aiApiStatus = msg)
            }.onFailure { ex ->
                _debugMetrics.value = _debugMetrics.value.copy(aiApiStatus = "Failed: ${ex.message}")
                recordError("AI API Test: ${ex.message}")
            }
        }
    }

    fun testNotification(): Boolean {
        return alertNotificationManager.sendTestNotification()
    }

    fun clearLastError() {
        _debugMetrics.value = _debugMetrics.value.copy(
            lastErrorMessage = null,
            lastErrorTimestamp = null
        )
        _marketErrorMessage.value = null
    }

    private fun recordError(msg: String) {
        Log.e(tag, "Logged Error: $msg")
        _debugMetrics.value = _debugMetrics.value.copy(
            lastErrorMessage = msg,
            lastErrorTimestamp = System.currentTimeMillis()
        )
    }

    private fun updateDebugNetwork(status: NetworkStatus) {
        _debugMetrics.value = _debugMetrics.value.copy(
            isInternetConnected = status.isConnected,
            networkType = "${status.networkType.name} (${if (status.isMetered) "Metered" else "Unmetered"})"
        )
    }
}
